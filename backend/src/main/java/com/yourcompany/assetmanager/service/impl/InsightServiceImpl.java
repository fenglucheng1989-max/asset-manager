package com.yourcompany.assetmanager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourcompany.assetmanager.dto.AssetMilestoneDTO;
import com.yourcompany.assetmanager.entity.AssetAccount;
import com.yourcompany.assetmanager.entity.AssetMilestone;
import com.yourcompany.assetmanager.entity.AssetSnapshot;
import com.yourcompany.assetmanager.entity.ReportSummary;
import com.yourcompany.assetmanager.entity.TransactionBudget;
import com.yourcompany.assetmanager.entity.TransactionCategory;
import com.yourcompany.assetmanager.entity.TransactionRecord;
import com.yourcompany.assetmanager.exception.BusinessException;
import com.yourcompany.assetmanager.mapper.AssetAccountMapper;
import com.yourcompany.assetmanager.mapper.AssetMilestoneMapper;
import com.yourcompany.assetmanager.mapper.AssetSnapshotMapper;
import com.yourcompany.assetmanager.mapper.ReportSummaryMapper;
import com.yourcompany.assetmanager.mapper.TransactionBudgetMapper;
import com.yourcompany.assetmanager.mapper.TransactionCategoryMapper;
import com.yourcompany.assetmanager.mapper.TransactionRecordMapper;
import com.yourcompany.assetmanager.service.AssetOverviewService;
import com.yourcompany.assetmanager.service.InsightService;
import com.yourcompany.assetmanager.utils.MoneyUtils;
import com.yourcompany.assetmanager.vo.AssetMilestoneVO;
import com.yourcompany.assetmanager.vo.AssetOverviewVO;
import com.yourcompany.assetmanager.vo.HealthScoreVO;
import com.yourcompany.assetmanager.vo.ReportSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightServiceImpl implements InsightService {

    private static final Set<String> LIQUID_ACCOUNT_TYPES = Set.of("CASH", "BANK", "E_WALLET");
    private static final Set<String> SHORT_TERM_LIABILITY_TYPES = Set.of("CREDIT_CARD", "OTHER_LIABILITY");
    private static final List<BigDecimal> NET_WORTH_THRESHOLDS = List.of(
            new BigDecimal("100000"),
            new BigDecimal("500000"),
            new BigDecimal("1000000"),
            new BigDecimal("2000000"),
            new BigDecimal("5000000"),
            new BigDecimal("10000000")
    );

    private final AssetAccountMapper assetAccountMapper;
    private final AssetSnapshotMapper assetSnapshotMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final TransactionCategoryMapper transactionCategoryMapper;
    private final TransactionBudgetMapper transactionBudgetMapper;
    private final ReportSummaryMapper reportSummaryMapper;
    private final AssetMilestoneMapper assetMilestoneMapper;
    private final AssetOverviewService assetOverviewService;
    private final ObjectMapper objectMapper;

    @Override
    public HealthScoreVO healthScore(Long userId) {
        AssetOverviewVO overview = assetOverviewService.getOverview(userId);
        List<AssetAccount> accounts = activeTotalAccounts(userId);
        boolean noAccounts = accounts.isEmpty();
        BigDecimal totalAsset = safe(overview.getTotalAsset());
        BigDecimal totalLiability = safe(overview.getTotalLiability());
        BigDecimal debtRatio = totalAsset.compareTo(BigDecimal.ZERO) == 0
                ? (totalLiability.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ONE : BigDecimal.ZERO)
                : ratio(totalLiability, totalAsset);

        BigDecimal liquidAssets = accounts.stream()
                .filter(account -> !Boolean.TRUE.equals(account.getIsLiability()))
                .filter(account -> LIQUID_ACCOUNT_TYPES.contains(account.getAccountType()))
                .map(this::baseAmount)
                .reduce(BigDecimal.ZERO, MoneyUtils::add);
        BigDecimal shortTermLiability = accounts.stream()
                .filter(account -> Boolean.TRUE.equals(account.getIsLiability()))
                .filter(account -> SHORT_TERM_LIABILITY_TYPES.contains(account.getAccountType()))
                .map(this::baseAmount)
                .reduce(BigDecimal.ZERO, MoneyUtils::add);
        BigDecimal liquidityRatio = shortTermLiability.compareTo(BigDecimal.ZERO) == 0
                ? (liquidAssets.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("9.99") : BigDecimal.ZERO)
                : ratio(liquidAssets, shortTermLiability);

        BigDecimal avgExpense = averageExpenseLastThreeMonths(userId);
        BigDecimal coverMonths = avgExpense.compareTo(BigDecimal.ZERO) == 0
                ? (liquidAssets.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("9.99") : BigDecimal.ZERO)
                : liquidAssets.divide(avgExpense, 2, RoundingMode.HALF_UP);

        YearMonth currentMonth = YearMonth.now();
        boolean noRecentRecords = transactionRecordMapper.selectCount(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .ge(TransactionRecord::getOccurredAt, currentMonth.minusMonths(2).atDay(1).atStartOfDay())
                .lt(TransactionRecord::getOccurredAt, currentMonth.plusMonths(1).atDay(1).atStartOfDay())) == 0;
        BigDecimal monthlyIncome = sumRecords(userId, currentMonth.atDay(1).atStartOfDay(), currentMonth.plusMonths(1).atDay(1).atStartOfDay(), "INCOME");
        BigDecimal monthlyExpense = sumRecords(userId, currentMonth.atDay(1).atStartOfDay(), currentMonth.plusMonths(1).atDay(1).atStartOfDay(), "EXPENSE");
        BigDecimal monthlyNet = MoneyUtils.subtract(monthlyIncome, monthlyExpense);

        int score = debtScore(debtRatio) + emergencyScore(coverMonths) + cashflowScore(monthlyNet, monthlyExpense) + liquidityScore(liquidityRatio);
        String grade = grade(score);
        String title = title(grade);
        List<HealthScoreVO.MetricVO> metrics = List.of(
                debtMetric(debtRatio),
                emergencyMetric(coverMonths),
                cashflowMetric(monthlyNet),
                liquidityMetric(liquidityRatio)
        );
        boolean dataInsufficient = noAccounts || noRecentRecords;
        String dataNotice = noAccounts
                ? "先添加资产或负债账户后，才能生成有效的健康评级。"
                : noRecentRecords ? "近 3 个月流水较少，当前评级仅供参考，继续记账后会更准确。" : null;
        return HealthScoreVO.builder()
                .score(dataInsufficient ? 0 : score)
                .grade(dataInsufficient ? "NA" : grade)
                .title(dataInsufficient ? "待完善" : title)
                .summary(dataInsufficient ? dataNotice : summary(grade, metrics))
                .metrics(noAccounts ? List.of() : metrics)
                .dataInsufficient(dataInsufficient)
                .dataNotice(dataNotice)
                .build();
    }

    @Override
    public List<ReportSummaryVO> summaries(Long userId, Integer limit) {
        generateMonthlySummary(userId, LocalDate.now());
        int safeLimit = limit == null ? 12 : Math.max(1, Math.min(24, limit));
        return reportSummaryMapper.selectList(new LambdaQueryWrapper<ReportSummary>()
                        .eq(ReportSummary::getUserId, userId)
                        .eq(ReportSummary::getReportType, "MONTHLY")
                        .orderByDesc(ReportSummary::getPeriodStart)
                        .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toSummaryVO)
                .toList();
    }

    @Override
    @Transactional
    public ReportSummaryVO generateMonthlySummary(Long userId, LocalDate monthDate) {
        YearMonth ym = YearMonth.from(monthDate == null ? LocalDate.now() : monthDate);
        LocalDate periodStart = ym.atDay(1);
        LocalDate periodEnd = ym.atEndOfMonth();
        LocalDateTime start = periodStart.atStartOfDay();
        LocalDateTime end = periodEnd.plusDays(1).atStartOfDay();
        List<TransactionRecord> records = transactionRecordMapper.selectList(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .ge(TransactionRecord::getOccurredAt, start)
                .lt(TransactionRecord::getOccurredAt, end)
                .orderByAsc(TransactionRecord::getOccurredAt));

        BigDecimal income = sumBase(records, "INCOME");
        BigDecimal expense = sumBase(records, "EXPENSE");
        BigDecimal endNetWorth = endNetWorth(userId, periodEnd);
        BigDecimal previousNetWorth = previousNetWorth(userId, periodStart);
        boolean comparisonAvailable = previousNetWorth != null;
        BigDecimal netWorthChange = previousNetWorth == null ? BigDecimal.ZERO : MoneyUtils.subtract(endNetWorth, previousNetWorth);
        BudgetUsage budget = budgetUsage(userId, ym);
        ReportSummaryVO.RecordItemVO largestIncome = largestRecord(records, "INCOME");
        ReportSummaryVO.RecordItemVO largestExpense = largestRecord(records, "EXPENSE");
        boolean insufficient = records.isEmpty();
        String title = ym.format(DateTimeFormatter.ofPattern("yyyy 年 M 月")) + "财务摘要";
        String description;
        if (insufficient) {
            description = "本月暂未记录流水，摘要会在继续记录后更完整。";
        } else if (!comparisonAvailable) {
            description = "本月期末净资产为 " + endNetWorth.setScale(2, RoundingMode.HALF_UP) + " 元，暂无上月快照用于环比。";
        } else {
            description = "本月净资产" + (netWorthChange.compareTo(BigDecimal.ZERO) >= 0 ? "增加" : "减少") + " " + netWorthChange.abs().setScale(2, RoundingMode.HALF_UP) + " 元。";
        }

        ReportSummaryVO vo = ReportSummaryVO.builder()
                .reportType("MONTHLY")
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .title(title)
                .description(description)
                .endNetWorth(endNetWorth)
                .netWorthChange(netWorthChange)
                .comparisonAvailable(comparisonAvailable)
                .income(income)
                .expense(expense)
                .budgetAmount(budget.amount())
                .budgetUsed(budget.used())
                .budgetUsageRate(budget.rate())
                .largestIncome(largestIncome)
                .largestExpense(largestExpense)
                .dataInsufficient(insufficient)
                .dataNotice("基于已记录流水生成，未记录的现金流不会纳入统计。")
                .build();
        ReportSummary summary = upsertSummary(userId, periodStart, periodEnd, vo);
        detectNetWorthMilestones(userId, endNetWorth, periodEnd);
        return toSummaryVO(summary);
    }

    @Override
    public List<AssetMilestoneVO> milestones(Long userId) {
        detectNetWorthMilestones(userId, assetOverviewService.getOverview(userId).getNetWorth(), LocalDate.now());
        return assetMilestoneMapper.selectList(new LambdaQueryWrapper<AssetMilestone>()
                        .eq(AssetMilestone::getUserId, userId)
                        .orderByDesc(AssetMilestone::getAchievedAt)
                        .orderByDesc(AssetMilestone::getId))
                .stream()
                .map(this::toMilestoneVO)
                .toList();
    }

    @Override
    @Transactional
    public AssetMilestoneVO createCustomMilestone(Long userId, AssetMilestoneDTO dto) {
        AssetMilestone milestone = AssetMilestone.builder()
                .userId(userId)
                .milestoneType("CUSTOM")
                .achievedAt(dto.getAchievedAt() == null ? LocalDate.now() : dto.getAchievedAt())
                .note(dto.getNote().trim())
                .build();
        assetMilestoneMapper.insert(milestone);
        return toMilestoneVO(milestone);
    }

    @Override
    @Transactional
    public void detectNetWorthMilestones(Long userId, BigDecimal netWorth, LocalDate achievedAt) {
        BigDecimal safeNetWorth = safe(netWorth);
        for (BigDecimal threshold : NET_WORTH_THRESHOLDS) {
            if (safeNetWorth.compareTo(threshold) < 0) continue;
            Long exists = assetMilestoneMapper.selectCount(new LambdaQueryWrapper<AssetMilestone>()
                    .eq(AssetMilestone::getUserId, userId)
                    .eq(AssetMilestone::getMilestoneType, "NET_WORTH")
                    .eq(AssetMilestone::getThreshold, threshold));
            if (exists > 0) continue;
            assetMilestoneMapper.insert(AssetMilestone.builder()
                    .userId(userId)
                    .milestoneType("NET_WORTH")
                    .threshold(threshold)
                    .achievedAt(achievedAt == null ? LocalDate.now() : achievedAt)
                    .note("净资产首次达到 " + threshold.setScale(0, RoundingMode.HALF_UP) + " 元")
                    .build());
        }
    }

    @Scheduled(cron = "${insight.summary.monthly-cron:0 10 0 1 * *}")
    public void generateCurrentMonthSummaries() {
        assetAccountMapper.selectList(null).stream()
                .map(AssetAccount::getUserId)
                .filter(id -> id != null)
                .distinct()
                .forEach(userId -> {
                    try {
                        generateMonthlySummary(userId, LocalDate.now().minusMonths(1));
                    } catch (RuntimeException e) {
                        log.warn("Generate monthly insight summary failed, userId={}", userId, e);
                    }
                });
    }

    private List<AssetAccount> activeTotalAccounts(Long userId) {
        return assetAccountMapper.selectList(new LambdaQueryWrapper<AssetAccount>()
                .eq(AssetAccount::getUserId, userId)
                .eq(AssetAccount::getArchived, false)
                .eq(AssetAccount::getIncludeInTotal, true));
    }

    private BigDecimal averageExpenseLastThreeMonths(Long userId) {
        YearMonth current = YearMonth.now();
        LocalDateTime start = current.minusMonths(2).atDay(1).atStartOfDay();
        LocalDateTime end = current.plusMonths(1).atDay(1).atStartOfDay();
        BigDecimal expense = sumRecords(userId, start, end, "EXPENSE");
        return expense.divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumRecords(Long userId, LocalDateTime start, LocalDateTime end, String type) {
        return sumBase(transactionRecordMapper.selectList(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .eq(TransactionRecord::getTransactionType, type)
                .ge(TransactionRecord::getOccurredAt, start)
                .lt(TransactionRecord::getOccurredAt, end)), type);
    }

    private BigDecimal sumBase(List<TransactionRecord> records, String type) {
        return records.stream()
                .filter(record -> type.equals(record.getTransactionType()))
                .map(record -> MoneyUtils.toBaseCurrency(record.getAmount(), record.getExchangeRateToCny()))
                .reduce(BigDecimal.ZERO, MoneyUtils::add);
    }

    private BigDecimal baseAmount(AssetAccount account) {
        return MoneyUtils.toBaseCurrency(account.getCurrentBalance(), account.getExchangeRateToCny()).abs();
    }

    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return safe(numerator).divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int debtScore(BigDecimal debtRatio) {
        if (debtRatio.compareTo(new BigDecimal("0.30")) <= 0) return 30;
        if (debtRatio.compareTo(new BigDecimal("0.60")) <= 0) return 20;
        return 8;
    }

    private int emergencyScore(BigDecimal coverMonths) {
        if (coverMonths.compareTo(new BigDecimal("6")) >= 0) return 30;
        if (coverMonths.compareTo(new BigDecimal("3")) >= 0) return 22;
        if (coverMonths.compareTo(BigDecimal.ONE) >= 0) return 12;
        return 4;
    }

    private int cashflowScore(BigDecimal monthlyNet, BigDecimal expense) {
        if (monthlyNet.compareTo(BigDecimal.ZERO) >= 0) return 25;
        BigDecimal tolerance = safe(expense).multiply(new BigDecimal("-0.10"));
        if (monthlyNet.compareTo(tolerance) >= 0) return 15;
        return 5;
    }

    private int liquidityScore(BigDecimal liquidityRatio) {
        if (liquidityRatio.compareTo(new BigDecimal("2")) >= 0) return 15;
        if (liquidityRatio.compareTo(BigDecimal.ONE) >= 0) return 10;
        return 4;
    }

    private String grade(int score) {
        if (score >= 85) return "A";
        if (score >= 70) return "B";
        if (score >= 55) return "C";
        return "D";
    }

    private String title(String grade) {
        return switch (grade) {
            case "A" -> "健康";
            case "B" -> "稳健";
            case "C" -> "需关注";
            default -> "风险偏高";
        };
    }

    private String summary(String grade, List<HealthScoreVO.MetricVO> metrics) {
        String firstConcern = metrics.stream()
                .filter(metric -> !"GOOD".equals(metric.getLevel()))
                .map(HealthScoreVO.MetricVO::getName)
                .findFirst()
                .orElse("资产结构");
        if ("A".equals(grade)) return "整体状态健康，继续保持稳定记录和预算管理。";
        if ("B".equals(grade)) return "整体较稳健，可继续优化" + firstConcern + "。";
        if ("C".equals(grade)) return firstConcern + "需要关注，建议先处理最容易改善的一项。";
        return "当前风险偏高，建议优先降低负债压力并补足流动资产。";
    }

    private HealthScoreVO.MetricVO debtMetric(BigDecimal value) {
        String level = value.compareTo(new BigDecimal("0.30")) <= 0 ? "GOOD" : value.compareTo(new BigDecimal("0.60")) <= 0 ? "WARN" : "RISK";
        return HealthScoreVO.MetricVO.builder()
                .key("DEBT_RATIO")
                .name("资产负债率")
                .value(value)
                .unit("%")
                .level(level)
                .conclusion("当前负债约占资产的 " + percent(value) + "。")
                .suggestion("优先让负债率保持在 30% 以下，超过 60% 时需要谨慎新增负债。")
                .build();
    }

    private HealthScoreVO.MetricVO emergencyMetric(BigDecimal value) {
        String level = value.compareTo(new BigDecimal("3")) >= 0 ? "GOOD" : value.compareTo(BigDecimal.ONE) >= 0 ? "WARN" : "RISK";
        return HealthScoreVO.MetricVO.builder()
                .key("EMERGENCY_MONTHS")
                .name("紧急资金覆盖")
                .value(value)
                .unit("个月")
                .level(level)
                .conclusion("流动资产约可覆盖 " + value.setScale(1, RoundingMode.HALF_UP) + " 个月支出。")
                .suggestion("建议保留 3-6 个月刚性支出的流动资产。")
                .build();
    }

    private HealthScoreVO.MetricVO cashflowMetric(BigDecimal value) {
        String level = value.compareTo(BigDecimal.ZERO) >= 0 ? "GOOD" : "RISK";
        return HealthScoreVO.MetricVO.builder()
                .key("MONTHLY_NET")
                .name("本月收支平衡")
                .value(value)
                .unit("元")
                .level(level)
                .conclusion("本月已记录收支净额为 " + value.setScale(2, RoundingMode.HALF_UP) + " 元。")
                .suggestion("如果长期为负，建议先检查高频支出和预算执行情况。")
                .build();
    }

    private HealthScoreVO.MetricVO liquidityMetric(BigDecimal value) {
        String level = value.compareTo(BigDecimal.ONE) >= 0 ? "GOOD" : "WARN";
        return HealthScoreVO.MetricVO.builder()
                .key("LIQUIDITY_RATIO")
                .name("流动比率")
                .value(value)
                .unit("倍")
                .level(level)
                .conclusion("流动资产约为短期负债的 " + value.setScale(2, RoundingMode.HALF_UP) + " 倍。")
                .suggestion("建议让流动资产至少覆盖短期负债。")
                .build();
    }

    private String percent(BigDecimal value) {
        return value.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP) + "%";
    }

    private BigDecimal endNetWorth(Long userId, LocalDate periodEnd) {
        AssetSnapshot snapshot = assetSnapshotMapper.selectOne(new LambdaQueryWrapper<AssetSnapshot>()
                .eq(AssetSnapshot::getUserId, userId)
                .le(AssetSnapshot::getSnapshotDate, periodEnd)
                .orderByDesc(AssetSnapshot::getSnapshotDate)
                .last("LIMIT 1"));
        if (snapshot != null) return safe(snapshot.getNetWorth());
        return safe(assetOverviewService.getOverview(userId).getNetWorth());
    }

    private BigDecimal previousNetWorth(Long userId, LocalDate periodStart) {
        AssetSnapshot snapshot = assetSnapshotMapper.selectOne(new LambdaQueryWrapper<AssetSnapshot>()
                .eq(AssetSnapshot::getUserId, userId)
                .lt(AssetSnapshot::getSnapshotDate, periodStart)
                .orderByDesc(AssetSnapshot::getSnapshotDate)
                .last("LIMIT 1"));
        return snapshot == null ? null : safe(snapshot.getNetWorth());
    }

    private BudgetUsage budgetUsage(Long userId, YearMonth ym) {
        String month = ym.toString();
        List<TransactionBudget> budgets = transactionBudgetMapper.selectList(new LambdaQueryWrapper<TransactionBudget>()
                .eq(TransactionBudget::getUserId, userId)
                .eq(TransactionBudget::getBudgetMonth, month)
                .eq(TransactionBudget::getBudgetType, "EXPENSE"));
        BigDecimal amount = budgets.stream().map(TransactionBudget::getAmount).reduce(BigDecimal.ZERO, MoneyUtils::add);
        BigDecimal used = sumRecords(userId, ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay(), "EXPENSE");
        BigDecimal rate = amount.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : used.divide(amount, 4, RoundingMode.HALF_UP);
        return new BudgetUsage(amount, used, rate);
    }

    private ReportSummaryVO.RecordItemVO largestRecord(List<TransactionRecord> records, String type) {
        Map<Long, TransactionCategory> categories = transactionCategoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(TransactionCategory::getId, Function.identity(), (a, b) -> a));
        return records.stream()
                .filter(record -> type.equals(record.getTransactionType()))
                .max(Comparator.comparing(record -> MoneyUtils.toBaseCurrency(record.getAmount(), record.getExchangeRateToCny())))
                .map(record -> {
                    TransactionCategory category = categories.get(record.getCategoryId());
                    return ReportSummaryVO.RecordItemVO.builder()
                            .id(record.getId())
                            .name(category == null ? ("INCOME".equals(type) ? "收入" : "支出") : category.getName())
                            .amount(MoneyUtils.toBaseCurrency(record.getAmount(), record.getExchangeRateToCny()))
                            .occurredAt(record.getOccurredAt() == null ? null : record.getOccurredAt().toString())
                            .build();
                })
                .orElse(null);
    }

    private ReportSummary upsertSummary(Long userId, LocalDate periodStart, LocalDate periodEnd, ReportSummaryVO vo) {
        ReportSummary summary = reportSummaryMapper.selectOne(new LambdaQueryWrapper<ReportSummary>()
                .eq(ReportSummary::getUserId, userId)
                .eq(ReportSummary::getReportType, "MONTHLY")
                .eq(ReportSummary::getPeriodStart, periodStart)
                .eq(ReportSummary::getPeriodEnd, periodEnd));
        String json = writeSummary(vo);
        if (summary == null) {
            summary = ReportSummary.builder()
                    .userId(userId)
                    .reportType("MONTHLY")
                    .periodStart(periodStart)
                    .periodEnd(periodEnd)
                    .summaryJson(json)
                    .build();
            reportSummaryMapper.insert(summary);
        } else {
            summary.setSummaryJson(json);
            reportSummaryMapper.updateById(summary);
        }
        return summary;
    }

    private String writeSummary(ReportSummaryVO vo) {
        try {
            return objectMapper.writeValueAsString(vo);
        } catch (Exception e) {
            throw new BusinessException(500, "生成摘要失败");
        }
    }

    private ReportSummaryVO toSummaryVO(ReportSummary summary) {
        try {
            Map<String, Object> map = objectMapper.readValue(summary.getSummaryJson(), new TypeReference<Map<String, Object>>() {});
            ReportSummaryVO vo = objectMapper.convertValue(map, ReportSummaryVO.class);
            vo.setId(summary.getId());
            vo.setReportType(summary.getReportType());
            vo.setPeriodStart(summary.getPeriodStart());
            vo.setPeriodEnd(summary.getPeriodEnd());
            return vo;
        } catch (Exception e) {
            return ReportSummaryVO.builder()
                    .id(summary.getId())
                    .reportType(summary.getReportType())
                    .periodStart(summary.getPeriodStart())
                    .periodEnd(summary.getPeriodEnd())
                    .title("财务摘要")
                    .description("摘要数据解析失败，请重新生成。")
                    .dataInsufficient(true)
                    .dataNotice("基于已记录流水生成。")
                    .build();
        }
    }

    private AssetMilestoneVO toMilestoneVO(AssetMilestone milestone) {
        return AssetMilestoneVO.builder()
                .id(milestone.getId())
                .milestoneType(milestone.getMilestoneType())
                .threshold(milestone.getThreshold())
                .achievedAt(milestone.getAchievedAt())
                .note(milestone.getNote())
                .build();
    }

    private record BudgetUsage(BigDecimal amount, BigDecimal used, BigDecimal rate) {
    }
}
