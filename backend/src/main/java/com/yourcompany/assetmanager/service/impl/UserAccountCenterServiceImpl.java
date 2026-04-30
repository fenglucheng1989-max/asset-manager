package com.yourcompany.assetmanager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourcompany.assetmanager.entity.AppUser;
import com.yourcompany.assetmanager.entity.AssetAccount;
import com.yourcompany.assetmanager.entity.AssetAccountBalanceHistory;
import com.yourcompany.assetmanager.entity.AssetSnapshot;
import com.yourcompany.assetmanager.entity.TransactionBudget;
import com.yourcompany.assetmanager.entity.TransactionCategory;
import com.yourcompany.assetmanager.entity.TransactionRecord;
import com.yourcompany.assetmanager.exception.BusinessException;
import com.yourcompany.assetmanager.mapper.AppUserMapper;
import com.yourcompany.assetmanager.mapper.AssetAccountBalanceHistoryMapper;
import com.yourcompany.assetmanager.mapper.AssetAccountMapper;
import com.yourcompany.assetmanager.mapper.AssetSnapshotMapper;
import com.yourcompany.assetmanager.mapper.TransactionBudgetMapper;
import com.yourcompany.assetmanager.mapper.TransactionCategoryMapper;
import com.yourcompany.assetmanager.mapper.TransactionRecordMapper;
import com.yourcompany.assetmanager.service.AssetOverviewService;
import com.yourcompany.assetmanager.service.UserAccountCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class UserAccountCenterServiceImpl implements UserAccountCenterService {

    private final AssetAccountMapper assetAccountMapper;
    private final AssetSnapshotMapper assetSnapshotMapper;
    private final AssetAccountBalanceHistoryMapper balanceHistoryMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final TransactionBudgetMapper transactionBudgetMapper;
    private final TransactionCategoryMapper transactionCategoryMapper;
    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AssetOverviewService assetOverviewService;
    private final ObjectMapper objectMapper;

    @Override
    public String exportCsv(Long userId, String type) {
        return switch (normalizeType(type)) {
            case "accounts" -> exportAccounts(userId);
            case "transactions" -> exportTransactions(userId);
            case "budgets" -> exportBudgets(userId);
            case "snapshots" -> exportSnapshots(userId);
            default -> throw new BusinessException(400, "不支持的导出类型");
        };
    }

    @Override
    public Map<String, Object> exportBackup(Long userId) {
        return Map.of(
                "version", 1,
                "exportedAt", java.time.LocalDateTime.now().toString(),
                "accounts", assetAccountMapper.selectList(new LambdaQueryWrapper<AssetAccount>()
                        .eq(AssetAccount::getUserId, userId)
                        .orderByAsc(AssetAccount::getSortOrder)
                        .orderByAsc(AssetAccount::getId)),
                "balanceHistories", balanceHistoryMapper.selectList(new LambdaQueryWrapper<AssetAccountBalanceHistory>()
                        .eq(AssetAccountBalanceHistory::getUserId, userId)
                        .orderByAsc(AssetAccountBalanceHistory::getCreatedAt)
                        .orderByAsc(AssetAccountBalanceHistory::getId)),
                "snapshots", assetSnapshotMapper.selectList(new LambdaQueryWrapper<AssetSnapshot>()
                        .eq(AssetSnapshot::getUserId, userId)
                        .orderByAsc(AssetSnapshot::getSnapshotDate)),
                "categories", transactionCategoryMapper.selectList(new LambdaQueryWrapper<TransactionCategory>()
                        .eq(TransactionCategory::getUserId, userId)
                        .orderByAsc(TransactionCategory::getSortOrder)
                        .orderByAsc(TransactionCategory::getId)),
                "transactions", transactionRecordMapper.selectList(new LambdaQueryWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getUserId, userId)
                        .orderByAsc(TransactionRecord::getOccurredAt)
                        .orderByAsc(TransactionRecord::getId)),
                "budgets", transactionBudgetMapper.selectList(new LambdaQueryWrapper<TransactionBudget>()
                        .eq(TransactionBudget::getUserId, userId)
                        .orderByAsc(TransactionBudget::getBudgetMonth)
                        .orderByAsc(TransactionBudget::getId))
        );
    }

    @Override
    @Transactional
    public void restoreBackup(Long userId, Map<String, Object> backup) {
        if (backup == null || backup.isEmpty()) {
            throw new BusinessException(400, "备份文件为空");
        }

        List<AssetAccount> accounts = convertList(backup.get("accounts"), new TypeReference<List<AssetAccount>>() {});
        List<AssetAccountBalanceHistory> histories = convertList(backup.get("balanceHistories"), new TypeReference<List<AssetAccountBalanceHistory>>() {});
        List<AssetSnapshot> snapshots = convertList(backup.get("snapshots"), new TypeReference<List<AssetSnapshot>>() {});
        List<TransactionCategory> categories = convertList(backup.get("categories"), new TypeReference<List<TransactionCategory>>() {});
        List<TransactionRecord> transactions = convertList(backup.get("transactions"), new TypeReference<List<TransactionRecord>>() {});
        List<TransactionBudget> budgets = convertList(backup.get("budgets"), new TypeReference<List<TransactionBudget>>() {});

        clearUserData(userId);

        Map<Long, Long> accountIdMap = new HashMap<>();
        for (AssetAccount account : accounts) {
            Long oldId = account.getId();
            account.setId(null);
            account.setUserId(userId);
            assetAccountMapper.insert(account);
            if (oldId != null) accountIdMap.put(oldId, account.getId());
        }

        Map<Long, Long> categoryIdMap = new HashMap<>();
        for (TransactionCategory category : categories) {
            Long oldId = category.getId();
            category.setId(null);
            category.setUserId(userId);
            transactionCategoryMapper.insert(category);
            if (oldId != null) categoryIdMap.put(oldId, category.getId());
        }

        for (AssetSnapshot snapshot : snapshots) {
            snapshot.setId(null);
            snapshot.setUserId(userId);
            assetSnapshotMapper.insert(snapshot);
        }

        for (TransactionBudget budget : budgets) {
            budget.setId(null);
            budget.setUserId(userId);
            budget.setCategoryId(remapNullable(categoryIdMap, budget.getCategoryId()));
            transactionBudgetMapper.insert(budget);
        }

        for (TransactionRecord record : transactions) {
            record.setId(null);
            record.setUserId(userId);
            record.setAccountId(remapNullable(accountIdMap, record.getAccountId()));
            record.setTargetAccountId(remapNullable(accountIdMap, record.getTargetAccountId()));
            record.setCategoryId(remapNullable(categoryIdMap, record.getCategoryId()));
            transactionRecordMapper.insert(record);
        }

        for (AssetAccountBalanceHistory history : histories) {
            Long nextAccountId = remapNullable(accountIdMap, history.getAccountId());
            if (nextAccountId == null) continue;
            history.setId(null);
            history.setUserId(userId);
            history.setAccountId(nextAccountId);
            balanceHistoryMapper.insert(history);
        }

        assetOverviewService.evictOverviewCache(userId);
    }

    @Override
    @Transactional
    public void clearUserData(Long userId) {
        transactionRecordMapper.delete(userWrapper(TransactionRecord::getUserId, userId));
        transactionBudgetMapper.delete(userWrapper(TransactionBudget::getUserId, userId));
        transactionCategoryMapper.delete(userWrapper(TransactionCategory::getUserId, userId));
        balanceHistoryMapper.delete(userWrapper(AssetAccountBalanceHistory::getUserId, userId));
        assetSnapshotMapper.delete(userWrapper(AssetSnapshot::getUserId, userId));
        assetAccountMapper.delete(userWrapper(AssetAccount::getUserId, userId));
        assetOverviewService.evictOverviewCache(userId);
    }

    @Override
    @Transactional
    public void changePassword(AppUser user, String currentPassword, String newPassword) {
        ensurePasswordMatches(user, currentPassword);
        if (Objects.equals(currentPassword, newPassword)) {
            throw new BusinessException(400, "新密码不能与当前密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        appUserMapper.updateById(user);
    }

    @Override
    @Transactional
    public void deleteAccount(AppUser user, String currentPassword) {
        ensurePasswordMatches(user, currentPassword);
        clearUserData(user.getId());
        appUserMapper.deleteById(user.getId());
    }

    private String exportAccounts(Long userId) {
        List<AssetAccount> rows = assetAccountMapper.selectList(new LambdaQueryWrapper<AssetAccount>()
                .eq(AssetAccount::getUserId, userId)
                .orderByAsc(AssetAccount::getSortOrder)
                .orderByAsc(AssetAccount::getId));
        return csv(
                List.of("账户ID", "账户名称", "账户类型", "币种", "兑人民币汇率", "当前余额", "是否负债", "计入总资产", "是否归档", "备注", "创建时间", "更新时间"),
                rows.stream().map(row -> Arrays.asList(
                        row.getId(), row.getName(), accountTypeName(row.getAccountType()), row.getCurrency(), row.getExchangeRateToCny(),
                        row.getCurrentBalance(), yesNo(row.getIsLiability()), yesNo(row.getIncludeInTotal()), yesNo(row.getArchived()),
                        row.getRemark(), row.getCreatedAt(), row.getUpdatedAt())).toList());
    }

    private String exportTransactions(Long userId) {
        List<TransactionRecord> rows = transactionRecordMapper.selectList(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .orderByDesc(TransactionRecord::getOccurredAt)
                .orderByDesc(TransactionRecord::getId));
        return csv(
                List.of("流水ID", "类型", "账户ID", "目标账户ID", "分类ID", "金额", "币种", "兑人民币汇率", "发生时间", "标签", "备注", "创建时间"),
                rows.stream().map(row -> Arrays.asList(
                        row.getId(), transactionTypeName(row.getTransactionType()), row.getAccountId(), row.getTargetAccountId(),
                        row.getCategoryId(), row.getAmount(), row.getCurrency(), row.getExchangeRateToCny(),
                        row.getOccurredAt(), row.getTag(), row.getRemark(), row.getCreatedAt())).toList());
    }

    private String exportBudgets(Long userId) {
        List<TransactionBudget> rows = transactionBudgetMapper.selectList(new LambdaQueryWrapper<TransactionBudget>()
                .eq(TransactionBudget::getUserId, userId)
                .orderByDesc(TransactionBudget::getBudgetMonth)
                .orderByAsc(TransactionBudget::getId));
        return csv(
                List.of("预算ID", "预算月份", "分类ID", "预算类型", "金额", "预警比例", "备注", "创建时间", "更新时间"),
                rows.stream().map(row -> Arrays.asList(
                        row.getId(), row.getBudgetMonth(), row.getCategoryId(), transactionTypeName(row.getBudgetType()), row.getAmount(),
                        row.getWarningRate(), row.getRemark(), row.getCreatedAt(), row.getUpdatedAt())).toList());
    }

    private String exportSnapshots(Long userId) {
        List<AssetSnapshot> rows = assetSnapshotMapper.selectList(new LambdaQueryWrapper<AssetSnapshot>()
                .eq(AssetSnapshot::getUserId, userId)
                .orderByDesc(AssetSnapshot::getSnapshotDate));
        return csv(
                List.of("快照ID", "快照日期", "总资产", "总负债", "净资产", "创建时间"),
                rows.stream().map(row -> Arrays.asList(
                        row.getId(), row.getSnapshotDate(), row.getTotalAsset(), row.getTotalLiability(),
                        row.getNetWorth(), row.getCreatedAt())).toList());
    }

    private String csv(List<String> headers, List<? extends List<?>> rows) {
        String headerLine = headers.stream().map(this::escapeCsv).collect(Collectors.joining(","));
        String rowLines = rows.stream()
                .map(row -> row.stream().map(this::escapeCsv).collect(Collectors.joining(",")))
                .collect(Collectors.joining("\n"));
        return rowLines.isBlank() ? headerLine + "\n" : headerLine + "\n" + rowLines + "\n";
    }

    private String escapeCsv(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private String yesNo(Boolean value) {
        if (value == null) return "";
        return value ? "是" : "否";
    }

    private String accountTypeName(String type) {
        if (type == null) return "";
        return switch (type) {
            case "CASH" -> "现金";
            case "BANK" -> "银行卡";
            case "ALIPAY" -> "支付宝";
            case "WECHAT" -> "微信";
            case "INVESTMENT" -> "投资账户";
            case "CREDIT_CARD" -> "信用卡";
            case "LOAN" -> "贷款";
            default -> type;
        };
    }

    private String transactionTypeName(String type) {
        if (type == null) return "";
        return switch (type) {
            case "INCOME" -> "收入";
            case "EXPENSE" -> "支出";
            case "TRANSFER" -> "转账";
            default -> type;
        };
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw new BusinessException(400, "请选择导出类型");
        }
        return type.trim().toLowerCase(Locale.ROOT);
    }

    private <T> List<T> convertList(Object value, TypeReference<List<T>> typeReference) {
        if (value == null) return List.of();
        return objectMapper.convertValue(value, typeReference);
    }

    private Long remapNullable(Map<Long, Long> mapping, Long oldId) {
        if (oldId == null) return null;
        return mapping.get(oldId);
    }

    private void ensurePasswordMatches(AppUser user, String currentPassword) {
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException(400, "当前密码不正确");
        }
    }

    private <T> LambdaQueryWrapper<T> userWrapper(com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, Long> column, Long userId) {
        return new LambdaQueryWrapper<T>().eq(column, userId);
    }
}
