package com.yourcompany.assetmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AssetAccountDTO {

    @NotBlank(message = "账户名称不能为空")
    @Size(max = 50, message = "账户名称不能超过50个字符")
    private String name;

    @NotBlank(message = "账户类型不能为空")
    private String accountType;

    private String currency = "CNY";

    @DecimalMin(value = "0.00000001", message = "折算汇率必须大于0")
    private BigDecimal exchangeRateToCny = BigDecimal.ONE;

    @NotNull(message = "当前余额不能为空")
    private BigDecimal currentBalance;

    private Boolean isLiability = false;

    private Boolean includeInTotal = true;

    private Boolean archived = false;

    private String icon;

    private String colorHex;

    private Integer sortOrder = 0;

    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;
}
