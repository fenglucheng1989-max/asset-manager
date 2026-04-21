package com.yourcompany.assetmanager.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardVO {

    private Long userCount;
    private Long accountCount;
    private Long assetAccountCount;
    private Long liabilityAccountCount;
    private BigDecimal totalAsset;
    private BigDecimal totalLiability;
    private BigDecimal netWorth;
}
