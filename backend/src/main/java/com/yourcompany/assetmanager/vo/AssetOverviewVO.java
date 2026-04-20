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
public class AssetOverviewVO {

    private BigDecimal totalAsset;
    private BigDecimal totalLiability;
    private BigDecimal netWorth;
    private Integer accountCount;
    private LocalDateTime lastUpdateTime;
}
