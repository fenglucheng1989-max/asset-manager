package com.yourcompany.assetmanager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yourcompany.assetmanager.dto.AssetAccountDTO;
import com.yourcompany.assetmanager.entity.AssetAccount;
import com.yourcompany.assetmanager.exception.BusinessException;
import com.yourcompany.assetmanager.mapper.AssetAccountMapper;
import com.yourcompany.assetmanager.service.AssetAccountService;
import com.yourcompany.assetmanager.service.AssetOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetAccountServiceImpl implements AssetAccountService {

    private final AssetAccountMapper assetAccountMapper;
    private final AssetOverviewService assetOverviewService;

    @Override
    @Transactional
    public AssetAccount createAccount(Long userId, AssetAccountDTO dto) {
        AssetAccount account = AssetAccount.builder()
                .userId(userId)
                .name(dto.getName())
                .accountType(dto.getAccountType())
                .currency(dto.getCurrency())
                .currentBalance(dto.getCurrentBalance())
                .isLiability(dto.getIsLiability())
                .includeInTotal(dto.getIncludeInTotal())
                .icon(dto.getIcon())
                .colorHex(dto.getColorHex())
                .sortOrder(dto.getSortOrder())
                .remark(dto.getRemark())
                .build();

        assetAccountMapper.insert(account);
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
            throw new BusinessException(404, "Account not found");
        }

        existing.setName(dto.getName());
        existing.setAccountType(dto.getAccountType());
        existing.setCurrency(dto.getCurrency());
        existing.setCurrentBalance(dto.getCurrentBalance());
        existing.setIsLiability(dto.getIsLiability());
        existing.setIncludeInTotal(dto.getIncludeInTotal());
        existing.setIcon(dto.getIcon());
        existing.setColorHex(dto.getColorHex());
        existing.setSortOrder(dto.getSortOrder());
        existing.setRemark(dto.getRemark());

        assetAccountMapper.updateById(existing);
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
            throw new BusinessException(404, "Account not found");
        }

        assetAccountMapper.deleteById(accountId);
        assetOverviewService.evictOverviewCache(userId);
    }

    @Override
    public List<AssetAccount> getAccounts(Long userId) {
        return assetAccountMapper.selectList(
                new LambdaQueryWrapper<AssetAccount>()
                        .eq(AssetAccount::getUserId, userId)
                        .orderByAsc(AssetAccount::getSortOrder));
    }

    @Override
    @Transactional
    public void updateSort(Long userId, List<Long> sortedIds) {
        for (int i = 0; i < sortedIds.size(); i++) {
            AssetAccount account = assetAccountMapper.selectOne(
                    new LambdaQueryWrapper<AssetAccount>()
                            .eq(AssetAccount::getId, sortedIds.get(i))
                            .eq(AssetAccount::getUserId, userId));
            if (account != null) {
                account.setSortOrder(i);
                assetAccountMapper.updateById(account);
            }
        }
        assetOverviewService.evictOverviewCache(userId);
    }
}
