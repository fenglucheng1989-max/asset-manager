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
public class AssetSnapshotVO {

    private Long id;

    private LocalDate snapshotDate;

    private BigDecimal totalAsset;

    private BigDecimal totalLiability;

    private BigDecimal netWorth;
}
