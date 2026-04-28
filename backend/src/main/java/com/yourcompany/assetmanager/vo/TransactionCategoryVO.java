package com.yourcompany.assetmanager.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCategoryVO {

    private Long id;
    private String name;
    private String type;
    private String colorHex;
    private Integer sortOrder;
    private Boolean systemDefault;
}
