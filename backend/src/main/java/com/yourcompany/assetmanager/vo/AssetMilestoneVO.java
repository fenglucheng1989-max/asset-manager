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
public class AssetMilestoneVO {

    private Long id;

    private String milestoneType;

    private BigDecimal threshold;

    private LocalDate achievedAt;

    private String note;
}
