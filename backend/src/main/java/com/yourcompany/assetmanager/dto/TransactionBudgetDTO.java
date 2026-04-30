package com.yourcompany.assetmanager.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionBudgetDTO {

    @NotBlank(message = "预算期间不能为空")
    private String budgetMonth;

    private String periodType = "MONTHLY";

    private Long categoryId;

    private String budgetType = "EXPENSE";

    @NotNull(message = "预算金额不能为空")
    @DecimalMin(value = "0.01", message = "预算金额必须大于0")
    private BigDecimal amount;

    @DecimalMin(value = "0.1", message = "预警比例不能小于10%")
    @DecimalMax(value = "1.0", message = "预警比例不能超过100%")
    private BigDecimal warningRate = new BigDecimal("0.8");

    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;
}
