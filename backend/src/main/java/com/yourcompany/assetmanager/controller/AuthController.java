package com.yourcompany.assetmanager.controller;

import com.yourcompany.assetmanager.dto.AuthLoginDTO;
import com.yourcompany.assetmanager.dto.AuthRegisterDTO;
import com.yourcompany.assetmanager.service.AuthService;
import com.yourcompany.assetmanager.vo.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<Map<String, String>> register(@Valid @RequestBody AuthRegisterDTO dto) {
        String token = authService.register(dto);
        return ApiResponse.success(Map.of("token", token));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@Valid @RequestBody AuthLoginDTO dto) {
        String token = authService.login(dto);
        return ApiResponse.success(Map.of("token", token));
    }
}
