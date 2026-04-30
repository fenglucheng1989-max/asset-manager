package com.yourcompany.assetmanager.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionBudgetVO {

    private Long id;
    private String budgetMonth;
    private String periodType;
    private Long categoryId;
    private String categoryName;
    private String budgetType;
    private BigDecimal amount;
    private BigDecimal usedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal usageRate;
    private BigDecimal warningRate;
    private Boolean warning;
    private String remark;
    private Boolean subordinate;
    private LocalDateTime createdAt;
}
