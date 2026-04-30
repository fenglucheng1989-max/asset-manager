package com.yourcompany.assetmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDTO {

    @NotBlank(message = "请输入当前密码")
    private String currentPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 50, message = "新密码长度应为 6-50 位")
    private String newPassword;
}
