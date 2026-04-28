package com.yourcompany.assetmanager.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReportVO {

    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal net;
    private List<CategoryStatVO> categoryStats;
    private List<TrendPointVO> trend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStatVO {
        private Long categoryId;
        private String categoryName;
        private String type;
        private String colorHex;
        private BigDecimal amount;
        private BigDecimal percent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPointVO {
        private String label;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal net;
    }
}
