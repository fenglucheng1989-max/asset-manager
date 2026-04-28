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
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("asset_account_balance_history")
public class AssetAccountBalanceHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long accountId;

    private String changeType;

    private BigDecimal beforeBalance;

    private BigDecimal afterBalance;

    private BigDecimal changeAmount;

    private String currency;

    private BigDecimal exchangeRateToCny;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
