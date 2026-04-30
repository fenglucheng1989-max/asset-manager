package com.yourcompany.assetmanager.controller;

import com.yourcompany.assetmanager.dto.AuthLoginDTO;
import com.yourcompany.assetmanager.dto.AuthRegisterDTO;
import com.yourcompany.assetmanager.dto.AuthResult;
import com.yourcompany.assetmanager.service.AuthService;
import com.yourcompany.assetmanager.vo.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthResult> register(@Valid @RequestBody AuthRegisterDTO dto) {
        return ApiResponse.success(authService.register(dto));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResult> login(@Valid @RequestBody AuthLoginDTO dto) {
        return ApiResponse.success(authService.login(dto));
    }
}
