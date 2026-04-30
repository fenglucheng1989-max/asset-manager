package com.yourcompany.assetmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserFeedbackDTO {

    @NotBlank(message = "请选择反馈类型")
    @Size(max = 30, message = "反馈类型过长")
    private String type;

    @NotBlank(message = "请输入反馈内容")
    @Size(max = 1000, message = "反馈内容不能超过 1000 字")
    private String content;

    @Size(max = 120, message = "联系方式不能超过 120 字")
    private String contact;
}
