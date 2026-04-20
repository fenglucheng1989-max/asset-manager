package com.yourcompany.assetmanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("asset_account")
public class AssetAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String accountType;

    private String currency;

    private BigDecimal currentBalance;

    private Boolean isLiability;

    private Boolean includeInTotal;

    private String icon;

    private String colorHex;

    private Integer sortOrder;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
