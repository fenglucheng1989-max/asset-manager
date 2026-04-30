package com.yourcompany.assetmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteAccountDTO {

    @NotBlank(message = "请输入当前密码")
    private String currentPassword;
}
