package com.yourcompany.assetmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TransactionCategoryDTO {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 30, message = "分类名称不能超过30个字符")
    private String name;

    @NotBlank(message = "分类类型不能为空")
    private String type;

    @Size(max = 7, message = "颜色格式不正确")
    private String colorHex;

    private Integer sortOrder;
}
