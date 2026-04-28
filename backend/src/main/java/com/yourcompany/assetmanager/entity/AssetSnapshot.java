package com.yourcompany.assetmanager.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("asset_snapshot")
public class AssetSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate snapshotDate;

    private BigDecimal totalAsset;

    private BigDecimal totalLiability;

    private BigDecimal netWorth;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
