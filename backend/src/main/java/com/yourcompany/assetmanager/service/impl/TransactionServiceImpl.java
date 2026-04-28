package com.yourcompany.assetmanager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yourcompany.assetmanager.dto.TransactionBudgetDTO;
import com.yourcompany.assetmanager.dto.TransactionCategoryDTO;
import com.yourcompany.assetmanager.dto.TransactionRecordDTO;
import com.yourcompany.assetmanager.entity.AssetAccount;
import com.yourcompany.assetmanager.entity.AssetAccountBalanceHistory;
import com.yourcompany.assetmanager.entity.TransactionBudget;
import com.yourcompany.assetmanager.entity.TransactionCategory;
import com.yourcompany.assetmanager.entity.TransactionRecord;
import com.yourcompany.assetmanager.exception.BusinessException;
import com.yourcompany.assetmanager.mapper.AssetAccountBalanceHistoryMapper;
import com.yourcompany.assetmanager.mapper.AssetAccountMapper;
import com.yourcompany.assetmanager.mapper.TransactionBudgetMapper;
import com.yourcompany.assetmanager.mapper.TransactionCategoryMapper;
import com.yourcompany.assetmanager.mapper.TransactionRecordMapper;
import com.yourcompany.assetmanager.service.AssetOverviewService;
import com.yourcompany.assetmanager.service.TransactionService;
import com.yourcompany.assetmanager.utils.MoneyUtils;
import com.yourcompany.assetmanager.vo.TransactionBudgetVO;
import com.yourcompany.assetmanager.vo.TransactionCategoryVO;
import com.yourcompany.assetmanager.vo.TransactionRecordVO;
import com.yourcompany.assetmanager.vo.TransactionReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final TransactionRecordMapper transactionRecordMapper;
    private final TransactionCategoryMapper transactionCategoryMapper;
    private final TransactionBudgetMapper transactionBudgetMapper;
    private final AssetAccountMapper assetAccountMapper;
    private final AssetAccountBalanceHistoryMapper balanceHistoryMapper;
    private final AssetOverviewService assetOverviewService;

    @Override
    public List<TransactionCategoryVO> listCategories(Long userId, String type) {
        LambdaQueryWrapper<TransactionCategory> wrapper = new LambdaQueryWrapper<TransactionCategory>()
                .and(w -> w.isNull(TransactionCategory::getUserId).or().eq(TransactionCategory::getUserId, userId))
                .orderByAsc(TransactionCategory::getSortOrder)
                .orderByAsc(TransactionCategory::getId);
        if (type != null && !type.isBlank()) {
            wrapper.eq(TransactionCategory::getType, normalizeType(type));
        }
        return transactionCategoryMapper.selectList(wrapper).stream().map(this::toCategoryVO).toList();
    }

    @Override
    @Transactional
    public TransactionCategoryVO createCategory(Long userId, TransactionCategoryDTO dto) {
        TransactionCategory category = TransactionCategory.builder()
                .userId(userId)
                .name(trimRequired(dto.getName(), "分类名称不能为空"))
                .type(normalizeType(dto.getType()))
                .colorHex(normalizeColor(dto.getColorHex()))
                .sortOrder(dto.getSortOrder() == null ? 100 : dto.getSortOrder())
                .build();
        transactionCategoryMapper.insert(category);
        return toCategoryVO(category);
    }

    @Override
    @Transactional
    public TransactionCategoryVO updateCategory(Long userId, Long categoryId, TransactionCategoryDTO dto) {
        TransactionCategory category = getOwnedCategory(userId, categoryId);
        category.setName(trimRequired(dto.getName(), "分类名称不能为空"));
        category.setType(normalizeType(dto.getType()));
        category.setColorHex(normalizeColor(dto.getColorHex()));
        category.setSortOrder(dto.getSortOrder() == null ? 100 : dto.getSortOrder());
        transactionCategoryMapper.updateById(category);
        return toCategoryVO(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        getOwnedCategory(userId, categoryId);
        Long usedCount = transactionRecordMapper.selectCount(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .eq(TransactionRecord::getCategoryId, categoryId));
        if (usedCount > 0) {
            throw new BusinessException(400, "分类已被流水使用，不能删除");
        }
        transactionBudgetMapper.delete(new LambdaQueryWrapper<TransactionBudget>()
                .eq(TransactionBudget::getUserId, userId)
                .eq(TransactionBudget::getCategoryId, categoryId));
        transactionCategoryMapper.deleteById(categoryId);
    }

    @Override
    public List<TransactionRecordVO> listRecords(Long userId, Integer limit, String type, Long accountId, Long categoryId, LocalDate startDate, LocalDate endDate) {
        int safeLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(MAX_LIMIT, limit));
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .orderByDesc(TransactionRecord::getOccurredAt)
                .orderByDesc(TransactionRecord::getId)
                .last("LIMIT " + safeLimit);
        if (type != null && !type.isBlank()) {
            wrapper.eq(TransactionRecord::getTransactionType, normalizeType(type));
        }
        if (accountId != null) {
            wrapper.and(w -> w.eq(TransactionRecord::getAccountId, accountId).or().eq(TransactionRecord::getTargetAccountId, accountId));
        }
        if (categoryId != null) {
            wrapper.eq(TransactionRecord::getCategoryId, categoryId);
        }
        if (startDate != null) {
            wrapper.ge(TransactionRecord::getOccurredAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(TransactionRecord::getOccurredAt, endDate.plusDays(1).atStartOfDay());
        }
        List<TransactionRecord> records = transactionRecordMapper.selectList(wrapper);
        return toRecordVOs(records);
    }

    @Override
    @Transactional
    public TransactionRecordVO createRecord(Long userId, TransactionRecordDTO dto) {
        TransactionRecord record = buildRecord(userId, dto);
        applyRecord(record, false);
        transactionRecordMapper.insert(record);
        assetOverviewService.evictOverviewCache(userId);
        return toRecordVOs(List.of(record)).get(0);
    }

    @Override
    @Transactional
    public TransactionRecordVO updateRecord(Long userId, Long recordId, TransactionRecordDTO dto) {
        TransactionRecord existing = getOwnedRecord(userId, recordId);
        applyRecord(existing, true);
        TransactionRecord next = buildRecord(userId, dto);
        next.setId(existing.getId());
        applyRecord(next, false);
        transactionRecordMapper.updateById(next);
        assetOverviewService.evictOverviewCache(userId);
        return toRecordVOs(List.of(next)).get(0);
    }

    @Override
    @Transactional
    public void deleteRecord(Long userId, Long recordId) {
        TransactionRecord existing = getOwnedRecord(userId, recordId);
        applyRecord(existing, true);
        transactionRecordMapper.deleteById(recordId);
        assetOverviewService.evictOverviewCache(userId);
    }

    @Override
    public List<TransactionBudgetVO> listBudgets(Long userId, String month) {
        String safeMonth = normalizeMonth(month);
        List<TransactionBudget> budgets = transactionBudgetMapper.selectList(new LambdaQueryWrapper<TransactionBudget>()
                .eq(TransactionBudget::getUserId, userId)
                .eq(TransactionBudget::getBudgetMonth, safeMonth)
                .orderByAsc(TransactionBudget::getCategoryId)
                .orderByAsc(TransactionBudget::getId));
        return toBudgetVOs(userId, safeMonth, budgets);
    }

    @Override
    @Transactional
    public TransactionBudgetVO saveBudget(Long userId, TransactionBudgetDTO dto) {
        String month = normalizeMonth(dto.getBudgetMonth());
        Long categoryId = dto.getCategoryId();
        if (categoryId != null) {
            getVisibleCategory(userId, categoryId);
        }
        String budgetType = normalizeBudgetType(dto.getBudgetType());
        LambdaQueryWrapper<TransactionBudget> budgetWrapper = new LambdaQueryWrapper<TransactionBudget>()
                .eq(TransactionBudget::getUserId, userId)
                .eq(TransactionBudget::getBudgetMonth, month)
                .eq(TransactionBudget::getBudgetType, budgetType);
        if (categoryId == null) {
            budgetWrapper.isNull(TransactionBudget::getCategoryId);
        } else {
            budgetWrapper.eq(TransactionBudget::getCategoryId, categoryId);
        }
        TransactionBudget budget = transactionBudgetMapper.selectOne(budgetWrapper);
        if (budget == null) {
            budget = TransactionBudget.builder()
                    .userId(userId)
                    .budgetMonth(month)
                    .categoryId(categoryId)
                    .budgetType(budgetType)
                    .build();
        }
        budget.setAmount(dto.getAmount());
        budget.setWarningRate(dto.getWarningRate() == null ? new BigDecimal("0.8") : dto.getWarningRate());
        budget.setRemark(trimToNull(dto.getRemark()));
        if (budget.getId() == null) {
            transactionBudgetMapper.insert(budget);
        } else {
            transactionBudgetMapper.updateById(budget);
        }
        return toBudgetVOs(userId, month, List.of(budget)).get(0);
    }

    @Override
    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        TransactionBudget budget = transactionBudgetMapper.selectOne(new LambdaQueryWrapper<TransactionBudget>()
                .eq(TransactionBudget::getId, budgetId)
                .eq(TransactionBudget::getUserId, userId));
        if (budget == null) throw new BusinessException(404, "预算不存在");
        transactionBudgetMapper.deleteById(budgetId);
    }

    @Override
    public TransactionReportVO report(Long userId, String month) {
        YearMonth ym = YearMonth.parse(normalizeMonth(month));
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        List<TransactionRecord> records = transactionRecordMapper.selectList(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .ge(TransactionRecord::getOccurredAt, start)
                .lt(TransactionRecord::getOccurredAt, end)
                .orderByAsc(TransactionRecord::getOccurredAt));
        Map<Long, TransactionCategory> categories = transactionCategoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(TransactionCategory::getId, Function.identity(), (a, b) -> a));
        BigDecimal income = sumBase(records, "INCOME");
        BigDecimal expense = sumBase(records, "EXPENSE");
        List<TransactionReportVO.CategoryStatVO> categoryStats = categoryStats(records, categories, expense);
        List<TransactionReportVO.TrendPointVO> trend = dailyTrend(ym, records);
        return TransactionReportVO.builder()
                .income(income)
                .expense(expense)
                .net(MoneyUtils.subtract(income, expense))
                .categoryStats(categoryStats)
                .trend(trend)
                .build();
    }

    private TransactionRecord buildRecord(Long userId, TransactionRecordDTO dto) {
        String type = normalizeType(dto.getTransactionType());
        validateAccounts(userId, type, dto.getAccountId(), dto.getTargetAccountId());
        return TransactionRecord.builder()
                .userId(userId)
                .transactionType(type)
                .accountId(dto.getAccountId())
                .targetAccountId(dto.getTargetAccountId())
                .categoryId(dto.getCategoryId())
                .amount(dto.getAmount())
                .currency(normalizeCurrency(dto.getCurrency()))
                .exchangeRateToCny(normalizeRate(dto.getExchangeRateToCny()))
                .occurredAt(dto.getOccurredAt() == null ? LocalDateTime.now() : dto.getOccurredAt())
                .tag(trimToNull(dto.getTag()))
                .remark(trimToNull(dto.getRemark()))
                .build();
    }

    private void applyRecord(TransactionRecord record, boolean reverse) {
        BigDecimal signedAmount = record.getAmount();
        if (reverse) signedAmount = signedAmount.negate();
        switch (record.getTransactionType()) {
            case "INCOME" -> adjustAccount(record.getUserId(), record.getAccountId(), signedAmount, reverse ? "REVERSE_INCOME" : "TRANSACTION_INCOME");
            case "EXPENSE" -> adjustAccount(record.getUserId(), record.getAccountId(), signedAmount.negate(), reverse ? "REVERSE_EXPENSE" : "TRANSACTION_EXPENSE");
            case "TRANSFER" -> {
                adjustAccount(record.getUserId(), record.getAccountId(), signedAmount.negate(), reverse ? "REVERSE_TRANSFER_OUT" : "TRANSACTION_TRANSFER_OUT");
                adjustAccount(record.getUserId(), record.getTargetAccountId(), signedAmount, reverse ? "REVERSE_TRANSFER_IN" : "TRANSACTION_TRANSFER_IN");
            }
            default -> throw new BusinessException(400, "不支持的流水类型");
        }
    }

    private void adjustAccount(Long userId, Long accountId, BigDecimal delta, String changeType) {
        AssetAccount account = getOwnedAccount(userId, accountId);
        BigDecimal beforeBalance = account.getCurrentBalance();
        account.setCurrentBalance(MoneyUtils.add(account.getCurrentBalance(), delta));
        assetAccountMapper.updateById(account);
        recordBalanceHistory(account, beforeBalance, account.getCurrentBalance(), changeType);
    }

    private void recordBalanceHistory(AssetAccount account,
                                      BigDecimal beforeBalance,
                                      BigDecimal afterBalance,
                                      String changeType) {
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
                .remark("流水记账")
                .build();
        balanceHistoryMapper.insert(history);
    }

    private void validateAccounts(Long userId, String type, Long accountId, Long targetAccountId) {
        if (accountId == null) throw new BusinessException(400, "请选择账户");
        getOwnedAccount(userId, accountId);
        if ("TRANSFER".equals(type)) {
            if (targetAccountId == null) throw new BusinessException(400, "请选择转入账户");
            if (Objects.equals(accountId, targetAccountId)) throw new BusinessException(400, "转出和转入账户不能相同");
            getOwnedAccount(userId, targetAccountId);
        }
    }

    private AssetAccount getOwnedAccount(Long userId, Long accountId) {
        AssetAccount account = assetAccountMapper.selectOne(
                new LambdaQueryWrapper<AssetAccount>()
                        .eq(AssetAccount::getId, accountId)
                        .eq(AssetAccount::getUserId, userId));
        if (account == null) throw new BusinessException(404, "账户不存在");
        return account;
    }

    private TransactionRecord getOwnedRecord(Long userId, Long recordId) {
        TransactionRecord record = transactionRecordMapper.selectOne(
                new LambdaQueryWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getId, recordId)
                        .eq(TransactionRecord::getUserId, userId));
        if (record == null) throw new BusinessException(404, "流水不存在");
        return record;
    }

    private TransactionCategory getOwnedCategory(Long userId, Long categoryId) {
        TransactionCategory category = transactionCategoryMapper.selectOne(
                new LambdaQueryWrapper<TransactionCategory>()
                        .eq(TransactionCategory::getId, categoryId)
                        .eq(TransactionCategory::getUserId, userId));
        if (category == null) throw new BusinessException(404, "分类不存在");
        return category;
    }

    private TransactionCategory getVisibleCategory(Long userId, Long categoryId) {
        TransactionCategory category = transactionCategoryMapper.selectOne(
                new LambdaQueryWrapper<TransactionCategory>()
                        .eq(TransactionCategory::getId, categoryId)
                        .and(w -> w.isNull(TransactionCategory::getUserId).or().eq(TransactionCategory::getUserId, userId)));
        if (category == null) throw new BusinessException(404, "分类不存在");
        return category;
    }

    private List<TransactionRecordVO> toRecordVOs(List<TransactionRecord> records) {
        Map<Long, AssetAccount> accounts = assetAccountMapper.selectList(null).stream()
                .collect(Collectors.toMap(AssetAccount::getId, Function.identity(), (a, b) -> a));
        Map<Long, TransactionCategory> categories = transactionCategoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(TransactionCategory::getId, Function.identity(), (a, b) -> a));
        return records.stream().map(record -> toRecordVO(record, accounts, categories)).toList();
    }

    private TransactionRecordVO toRecordVO(TransactionRecord record,
                                           Map<Long, AssetAccount> accounts,
                                           Map<Long, TransactionCategory> categories) {
        AssetAccount account = accounts.get(record.getAccountId());
        AssetAccount targetAccount = accounts.get(record.getTargetAccountId());
        TransactionCategory category = categories.get(record.getCategoryId());
        return TransactionRecordVO.builder()
                .id(record.getId())
                .transactionType(record.getTransactionType())
                .accountId(record.getAccountId())
                .accountName(account == null ? null : account.getName())
                .targetAccountId(record.getTargetAccountId())
                .targetAccountName(targetAccount == null ? null : targetAccount.getName())
                .categoryId(record.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .amount(record.getAmount())
                .currency(record.getCurrency())
                .exchangeRateToCny(record.getExchangeRateToCny())
                .baseAmount(MoneyUtils.toBaseCurrency(record.getAmount(), record.getExchangeRateToCny()))
                .occurredAt(record.getOccurredAt())
                .tag(record.getTag())
                .remark(record.getRemark())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private TransactionCategoryVO toCategoryVO(TransactionCategory category) {
        return TransactionCategoryVO.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .colorHex(category.getColorHex())
                .sortOrder(category.getSortOrder())
                .systemDefault(category.getUserId() == null)
                .build();
    }

    private List<TransactionBudgetVO> toBudgetVOs(Long userId, String month, List<TransactionBudget> budgets) {
        Map<Long, TransactionCategory> categories = transactionCategoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(TransactionCategory::getId, Function.identity(), (a, b) -> a));
        return budgets.stream().map(budget -> {
            BigDecimal used = usedBudgetAmount(userId, month, budget.getCategoryId(), budget.getBudgetType());
            BigDecimal usageRate = budget.getAmount().compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : used.divide(budget.getAmount(), 4, RoundingMode.HALF_UP);
            TransactionCategory category = categories.get(budget.getCategoryId());
            return TransactionBudgetVO.builder()
                    .id(budget.getId())
                    .budgetMonth(budget.getBudgetMonth())
                    .categoryId(budget.getCategoryId())
                    .categoryName(category == null ? "全部支出" : category.getName())
                    .budgetType(budget.getBudgetType())
                    .amount(budget.getAmount())
                    .usedAmount(used)
                    .remainingAmount(MoneyUtils.subtract(budget.getAmount(), used))
                    .usageRate(usageRate)
                    .warningRate(budget.getWarningRate())
                    .warning(usageRate.compareTo(budget.getWarningRate()) >= 0)
                    .remark(budget.getRemark())
                    .createdAt(budget.getCreatedAt())
                    .build();
        }).toList();
    }

    private BigDecimal usedBudgetAmount(Long userId, String month, Long categoryId, String budgetType) {
        YearMonth ym = YearMonth.parse(month);
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .eq(TransactionRecord::getTransactionType, budgetType)
                .ge(TransactionRecord::getOccurredAt, ym.atDay(1).atStartOfDay())
                .lt(TransactionRecord::getOccurredAt, ym.plusMonths(1).atDay(1).atStartOfDay());
        if (categoryId != null) {
            wrapper.eq(TransactionRecord::getCategoryId, categoryId);
        }
        return transactionRecordMapper.selectList(wrapper).stream()
                .map(record -> MoneyUtils.toBaseCurrency(record.getAmount(), record.getExchangeRateToCny()))
                .reduce(BigDecimal.ZERO, MoneyUtils::add);
    }

    private BigDecimal sumBase(List<TransactionRecord> records, String type) {
        return records.stream()
                .filter(record -> type.equals(record.getTransactionType()))
                .map(record -> MoneyUtils.toBaseCurrency(record.getAmount(), record.getExchangeRateToCny()))
                .reduce(BigDecimal.ZERO, MoneyUtils::add);
    }

    private List<TransactionReportVO.CategoryStatVO> categoryStats(List<TransactionRecord> records,
                                                                   Map<Long, TransactionCategory> categories,
                                                                   BigDecimal totalExpense) {
        Map<Long, BigDecimal> amounts = new HashMap<>();
        for (TransactionRecord record : records) {
            if (!"EXPENSE".equals(record.getTransactionType())) continue;
            Long key = record.getCategoryId() == null ? 0L : record.getCategoryId();
            amounts.merge(key, MoneyUtils.toBaseCurrency(record.getAmount(), record.getExchangeRateToCny()), MoneyUtils::add);
        }
        return amounts.entrySet().stream()
                .map(entry -> {
                    TransactionCategory category = categories.get(entry.getKey());
                    BigDecimal percent = totalExpense.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : entry.getValue().divide(totalExpense, 4, RoundingMode.HALF_UP);
                    return TransactionReportVO.CategoryStatVO.builder()
                            .categoryId(entry.getKey() == 0L ? null : entry.getKey())
                            .categoryName(category == null ? "未分类" : category.getName())
                            .type("EXPENSE")
                            .colorHex(category == null ? "#64748B" : category.getColorHex())
                            .amount(entry.getValue())
                            .percent(percent)
                            .build();
                })
                .sorted(Comparator.comparing(TransactionReportVO.CategoryStatVO::getAmount).reversed())
                .toList();
    }

    private List<TransactionReportVO.TrendPointVO> dailyTrend(YearMonth ym, List<TransactionRecord> records) {
        Map<Integer, List<TransactionRecord>> byDay = records.stream()
                .collect(Collectors.groupingBy(record -> record.getOccurredAt().getDayOfMonth()));
        List<TransactionReportVO.TrendPointVO> points = new ArrayList<>();
        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            List<TransactionRecord> dayRecords = byDay.getOrDefault(day, List.of());
            BigDecimal income = sumBase(dayRecords, "INCOME");
            BigDecimal expense = sumBase(dayRecords, "EXPENSE");
            points.add(TransactionReportVO.TrendPointVO.builder()
                    .label(String.valueOf(day))
                    .income(income)
                    .expense(expense)
                    .net(MoneyUtils.subtract(income, expense))
                    .build());
        }
        return points;
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase();
        if (!List.of("INCOME", "EXPENSE", "TRANSFER").contains(normalized)) {
            throw new BusinessException(400, "不支持的流水类型");
        }
        return normalized;
    }

    private String normalizeBudgetType(String type) {
        String normalized = type == null ? "EXPENSE" : type.trim().toUpperCase();
        if (!List.of("EXPENSE", "INCOME").contains(normalized)) {
            throw new BusinessException(400, "不支持的预算类型");
        }
        return normalized;
    }

    private String normalizeMonth(String month) {
        String value = month == null || month.isBlank()
                ? YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                : month.trim();
        try {
            YearMonth.parse(value);
            return value;
        } catch (Exception e) {
            throw new BusinessException(400, "月份格式应为 yyyy-MM");
        }
    }

    private String normalizeColor(String color) {
        if (color == null || color.isBlank()) return "#2EBD85";
        String value = color.trim();
        if (!value.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new BusinessException(400, "颜色格式不正确");
        }
        return value.toUpperCase();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) return "CNY";
        return currency.trim().toUpperCase();
    }

    private BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ONE;
        return rate;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String trimRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) throw new BusinessException(400, message);
        return normalized;
    }
}
