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
public class AdminAccountVO {

    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String accountType;
    private String currency;
    private BigDecimal currentBalance;
    private Boolean isLiability;
    private Boolean includeInTotal;
    private String colorHex;
    private Integer sortOrder;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
