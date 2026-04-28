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
public class AssetAccountBalanceHistoryVO {

    private Long id;
    private Long accountId;
    private String changeType;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private BigDecimal changeAmount;
    private String currency;
    private BigDecimal exchangeRateToCny;
    private String remark;
    private LocalDateTime createdAt;
}
