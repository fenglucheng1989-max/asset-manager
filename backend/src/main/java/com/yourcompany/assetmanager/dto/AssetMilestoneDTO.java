package com.yourcompany.assetmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AssetMilestoneDTO {

    private LocalDate achievedAt;

    @NotBlank(message = "里程碑内容不能为空")
    @Size(max = 200, message = "里程碑内容不能超过200个字符")
    private String note;
}
