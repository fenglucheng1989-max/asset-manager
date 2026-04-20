package com.yourcompany.assetmanager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yourcompany.assetmanager.dto.AuthLoginDTO;
import com.yourcompany.assetmanager.dto.AuthRegisterDTO;
import com.yourcompany.assetmanager.entity.AppUser;
import com.yourcompany.assetmanager.exception.BusinessException;
import com.yourcompany.assetmanager.mapper.AppUserMapper;
import com.yourcompany.assetmanager.security.JwtUtils;
import com.yourcompany.assetmanager.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public String register(AuthRegisterDTO dto) {
        AppUser existing = appUserMapper.selectOne(
                new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, dto.getUsername()));
        if (existing != null) {
            throw new BusinessException("Username already exists");
        }

        AppUser user = AppUser.builder()
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .build();

        appUserMapper.insert(user);

        return jwtUtils.generateToken(user.getUsername(), user.getId());
    }

    @Override
    public String login(AuthLoginDTO dto) {
        AppUser user = appUserMapper.selectOne(
                new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, dto.getUsername()));

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "Invalid username or password");
        }

        return jwtUtils.generateToken(user.getUsername(), user.getId());
    }
}
