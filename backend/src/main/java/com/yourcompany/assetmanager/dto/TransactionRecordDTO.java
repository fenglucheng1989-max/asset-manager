package com.yourcompany.assetmanager.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionRecordDTO {

    @NotBlank(message = "流水类型不能为空")
    private String transactionType;

    private Long accountId;

    private Long targetAccountId;

    private Long categoryId;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.0001", message = "金额必须大于0")
    private BigDecimal amount;

    private String currency = "CNY";

    @DecimalMin(value = "0.00000001", message = "折算汇率必须大于0")
    private BigDecimal exchangeRateToCny = BigDecimal.ONE;

    private LocalDateTime occurredAt;

    @Size(max = 50, message = "标签不能超过50个字符")
    private String tag;

    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;
}
