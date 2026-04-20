package com.yourcompany.assetmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AssetAccountDTO {

    @NotBlank(message = "Account name is required")
    @Size(max = 50, message = "Account name must not exceed 50 characters")
    private String name;

    @NotBlank(message = "Account type is required")
    private String accountType;

    private String currency = "CNY";

    @NotNull(message = "Current balance is required")
    private BigDecimal currentBalance;

    private Boolean isLiability = false;

    private Boolean includeInTotal = true;

    private String icon;

    private String colorHex;

    private Integer sortOrder = 0;

    @Size(max = 200, message = "Remark must not exceed 200 characters")
    private String remark;
}
