package com.yourcompany.assetmanager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yourcompany.assetmanager.dto.AssetAccountDTO;
import com.yourcompany.assetmanager.entity.AssetAccount;
import com.yourcompany.assetmanager.entity.AssetAccountBalanceHistory;
import com.yourcompany.assetmanager.exception.BusinessException;
import com.yourcompany.assetmanager.mapper.AssetAccountMapper;
import com.yourcompany.assetmanager.mapper.AssetAccountBalanceHistoryMapper;
import com.yourcompany.assetmanager.service.AssetAccountService;
import com.yourcompany.assetmanager.service.AssetOverviewService;
import com.yourcompany.assetmanager.utils.MoneyUtils;
import com.yourcompany.assetmanager.vo.AssetAccountBalanceHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetAccountServiceImpl implements AssetAccountService {

    private final AssetAccountMapper assetAccountMapper;
    private final AssetAccountBalanceHistoryMapper balanceHistoryMapper;
    private final AssetOverviewService assetOverviewService;

    @Override
    @Transactional
    public AssetAccount createAccount(Long userId, AssetAccountDTO dto) {
        AssetAccount account = AssetAccount.builder()
                .userId(userId)
                .name(dto.getName())
                .accountType(dto.getAccountType())
                .currency(normalizeCurrency(dto.getCurrency()))
                .exchangeRateToCny(normalizeRate(dto.getExchangeRateToCny()))
                .currentBalance(dto.getCurrentBalance())
                .isLiability(dto.getIsLiability())
                .includeInTotal(dto.getIncludeInTotal())
                .archived(Boolean.TRUE.equals(dto.getArchived()))
                .icon(dto.getIcon())
                .colorHex(dto.getColorHex())
                .sortOrder(resolveCreateSortOrder(userId, dto.getSortOrder()))
                .remark(dto.getRemark())
                .build();

        assetAccountMapper.insert(account);
        recordBalanceHistory(account, BigDecimal.ZERO, account.getCurrentBalance(), "CREATE", "创建账户");
        assetOverviewService.evictOverviewCache(userId);
        return account;
    }

    @Override
    @Transactional
    public AssetAccount updateAccount(Long userId, Long accountId, AssetAccountDTO dto) {
        AssetAccount existing = assetAccountMapper.selectOne(
                new LambdaQueryWrapper<AssetAccount>()
                        .eq(AssetAccount::getId, accountId)
                        .eq(AssetAccount::getUserId, userId));

        if (existing == null) {
            throw new BusinessException(404, "账户不存在");
        }

        BigDecimal beforeBalance = existing.getCurrentBalance();
        existing.setName(dto.getName());
        existing.setAccountType(dto.getAccountType());
        existing.setCurrency(normalizeCurrency(dto.getCurrency()));
        existing.setExchangeRateToCny(normalizeRate(dto.getExchangeRateToCny()));
        existing.setCurrentBalance(dto.getCurrentBalance());
        existing.setIsLiability(dto.getIsLiability());
        existing.setIncludeInTotal(dto.getIncludeInTotal());
        existing.setArchived(Boolean.TRUE.equals(dto.getArchived()));
        existing.setIcon(dto.getIcon());
        existing.setColorHex(dto.getColorHex());
        existing.setSortOrder(dto.getSortOrder());
        existing.setRemark(dto.getRemark());

        assetAccountMapper.updateById(existing);
        if (beforeBalance == null || beforeBalance.compareTo(existing.getCurrentBalance()) != 0) {
            recordBalanceHistory(existing, beforeBalance, existing.getCurrentBalance(), "MANUAL_ADJUST", "手动调整余额");
        }
        assetOverviewService.evictOverviewCache(userId);
        return existing;
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId, Long accountId) {
        AssetAccount existing = assetAccountMapper.selectOne(
                new LambdaQueryWrapper<AssetAccount>()
                        .eq(AssetAccount::getId, accountId)
                        .eq(AssetAccount::getUserId, userId));

        if (existing == null) {
            throw new BusinessException(404, "账户不存在");
        }

        assetAccountMapper.deleteById(accountId);
        assetOverviewService.evictOverviewCache(userId);
    }

    @Override
    public List<AssetAccount> getAccounts(Long userId) {
        return assetAccountMapper.selectList(
                new LambdaQueryWrapper<AssetAccount>()
                        .eq(AssetAccount::getUserId, userId)
                        .eq(AssetAccount::getArchived, false)
                        .orderByAsc(AssetAccount::getSortOrder)
                        .orderByDesc(AssetAccount::getId));
    }

    @Override
    public AssetAccount getAccount(Long userId, Long accountId) {
        AssetAccount account = assetAccountMapper.selectOne(
                new LambdaQueryWrapper<AssetAccount>()
                        .eq(AssetAccount::getId, accountId)
                        .eq(AssetAccount::getUserId, userId));
        if (account == null) {
            throw new BusinessException(404, "账户不存在");
        }
        return account;
    }

    @Override
    public List<AssetAccountBalanceHistoryVO> getBalanceHistory(Long userId, Long accountId, Integer limit) {
        getAccount(userId, accountId);
        int safeLimit = limit == null ? 50 : Math.max(1, Math.min(200, limit));
        return balanceHistoryMapper.selectList(
                        new LambdaQueryWrapper<AssetAccountBalanceHistory>()
                                .eq(AssetAccountBalanceHistory::getUserId, userId)
                                .eq(AssetAccountBalanceHistory::getAccountId, accountId)
                                .orderByDesc(AssetAccountBalanceHistory::getCreatedAt)
                                .orderByDesc(AssetAccountBalanceHistory::getId)
                                .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toHistoryVO)
                .toList();
    }

    @Override
    @Transactional
    public AssetAccount archiveAccount(Long userId, Long accountId, boolean archived) {
        AssetAccount existing = assetAccountMapper.selectOne(
                new LambdaQueryWrapper<AssetAccount>()
                        .eq(AssetAccount::getId, accountId)
                        .eq(AssetAccount::getUserId, userId));

        if (existing == null) {
            throw new BusinessException(404, "账户不存在");
        }

        existing.setArchived(archived);
        assetAccountMapper.updateById(existing);
        assetOverviewService.evictOverviewCache(userId);
        return existing;
    }

    @Override
    @Transactional
    public void updateSort(Long userId, List<Long> sortedIds) {
        for (int i = 0; i < sortedIds.size(); i++) {
            AssetAccount account = assetAccountMapper.selectOne(
                    new LambdaQueryWrapper<AssetAccount>()
                            .eq(AssetAccount::getId, sortedIds.get(i))
                            .eq(AssetAccount::getUserId, userId)
                            .eq(AssetAccount::getArchived, false));
            if (account != null) {
                account.setSortOrder(i);
                assetAccountMapper.updateById(account);
            }
        }
        assetOverviewService.evictOverviewCache(userId);
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) return "CNY";
        return currency.trim().toUpperCase();
    }

    private int resolveCreateSortOrder(Long userId, Integer requestedSortOrder) {
        if (requestedSortOrder != null) return requestedSortOrder;
        AssetAccount firstAccount = assetAccountMapper.selectOne(
                new LambdaQueryWrapper<AssetAccount>()
                        .eq(AssetAccount::getUserId, userId)
                        .eq(AssetAccount::getArchived, false)
                        .orderByAsc(AssetAccount::getSortOrder)
                        .last("LIMIT 1"));
        if (firstAccount == null || firstAccount.getSortOrder() == null) return 0;
        return firstAccount.getSortOrder() - 1;
    }

    private BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ONE;
        return rate;
    }

    private void recordBalanceHistory(AssetAccount account,
                                      BigDecimal beforeBalance,
                                      BigDecimal afterBalance,
                                      String changeType,
                                      String remark) {
        BigDecimal safeBefore = beforeBalance == null ? BigDecimal.ZERO : beforeBalance;
        BigDecimal safeAfter = afterBalance == null ? BigDecimal.ZERO : afterBalance;
        AssetAccountBalanceHistory history = AssetAccountBalanceHistory.builder()
                .userId(account.getUserId())
                .accountId(account.getId())
                .changeType(changeType)
                .beforeBalance(safeBefore)
                .afterBalance(safeAfter)
                .changeAmount(MoneyUtils.subtract(safeAfter, safeBefore))
                .currency(normalizeCurrency(account.getCurrency()))
                .exchangeRateToCny(normalizeRate(account.getExchangeRateToCny()))
                .remark(remark)
                .build();
        balanceHistoryMapper.insert(history);
    }

    private AssetAccountBalanceHistoryVO toHistoryVO(AssetAccountBalanceHistory history) {
        return AssetAccountBalanceHistoryVO.builder()
                .id(history.getId())
                .accountId(history.getAccountId())
                .changeType(history.getChangeType())
                .beforeBalance(history.getBeforeBalance())
                .afterBalance(history.getAfterBalance())
                .changeAmount(history.getChangeAmount())
                .currency(history.getCurrency())
                .exchangeRateToCny(history.getExchangeRateToCny())
                .remark(history.getRemark())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
