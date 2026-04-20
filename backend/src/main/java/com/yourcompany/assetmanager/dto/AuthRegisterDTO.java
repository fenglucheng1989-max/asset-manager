package com.yourcompany.assetmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRegisterDTO {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private String email;
}
