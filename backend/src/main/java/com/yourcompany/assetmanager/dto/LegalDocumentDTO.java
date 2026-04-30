package com.yourcompany.assetmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LegalDocumentDTO {

    @NotBlank(message = "请选择文档类型")
    private String docType;

    @NotBlank(message = "请输入标题")
    @Size(max = 100, message = "标题不能超过 100 字")
    private String title;

    @NotBlank(message = "请输入版本号")
    @Size(max = 30, message = "版本号不能超过 30 字")
    private String version;

    @NotNull(message = "请选择生效日期")
    private LocalDate effectiveDate;

    @NotBlank(message = "请输入文档内容")
    private String content;

    private Boolean enabled;
}
