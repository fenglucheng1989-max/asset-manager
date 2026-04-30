package com.yourcompany.assetmanager.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryVO {

    private Long id;

    private String reportType;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private String title;

    private String description;

    private BigDecimal endNetWorth;

    private BigDecimal netWorthChange;

    private Boolean comparisonAvailable;

    private BigDecimal income;

    private BigDecimal expense;

    private BigDecimal budgetAmount;

    private BigDecimal budgetUsed;

    private BigDecimal budgetUsageRate;

    private RecordItemVO largestIncome;

    private RecordItemVO largestExpense;

    private Boolean dataInsufficient;

    private String dataNotice;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordItemVO {
        private Long id;
        private String name;
        private BigDecimal amount;
        private String occurredAt;
    }
}
