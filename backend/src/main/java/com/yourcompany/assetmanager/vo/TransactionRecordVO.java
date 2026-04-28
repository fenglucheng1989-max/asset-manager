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
public class TransactionRecordVO {

    private Long id;
    private String transactionType;
    private Long accountId;
    private String accountName;
    private Long targetAccountId;
    private String targetAccountName;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private String currency;
    private BigDecimal exchangeRateToCny;
    private BigDecimal baseAmount;
    private LocalDateTime occurredAt;
    private String tag;
    private String remark;
    private LocalDateTime createdAt;
}
