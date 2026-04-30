package com.yourcompany.assetmanager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yourcompany.assetmanager.dto.AuthLoginDTO;
import com.yourcompany.assetmanager.dto.AuthRegisterDTO;
import com.yourcompany.assetmanager.entity.AppUser;
import com.yourcompany.assetmanager.entity.LegalDocument;
import com.yourcompany.assetmanager.exception.BusinessException;
import com.yourcompany.assetmanager.mapper.AppUserMapper;
import com.yourcompany.assetmanager.mapper.LegalDocumentMapper;
import com.yourcompany.assetmanager.security.JwtUtils;
import com.yourcompany.assetmanager.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AppUserMapper appUserMapper;
    private final LegalDocumentMapper legalDocumentMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public String register(AuthRegisterDTO dto) {
        ensureLegalAccepted(dto);

        AppUser existing = appUserMapper.selectOne(
                new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, dto.getUsername()));
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }

        AppUser user = AppUser.builder()
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .role("USER")
                .acceptedTermsVersion(dto.getAcceptedTermsVersion())
                .acceptedPrivacyVersion(dto.getAcceptedPrivacyVersion())
                .legalAcceptedAt(LocalDateTime.now())
                .build();

        appUserMapper.insert(user);

        return jwtUtils.generateToken(user.getUsername(), user.getId());
    }

    private void ensureLegalAccepted(AuthRegisterDTO dto) {
        if (!Boolean.TRUE.equals(dto.getAcceptLegal())) {
            throw new BusinessException(400, "请先阅读并同意用户协议和隐私政策");
        }
        LegalDocument terms = latestEnabledDocument("TERMS");
        LegalDocument privacy = latestEnabledDocument("PRIVACY");
        if (terms == null || privacy == null) {
            throw new BusinessException(500, "用户协议或隐私政策未配置");
        }
        if (!terms.getVersion().equals(dto.getAcceptedTermsVersion())
                || !privacy.getVersion().equals(dto.getAcceptedPrivacyVersion())) {
            throw new BusinessException(400, "请确认最新版本的用户协议和隐私政策");
        }
    }

    private LegalDocument latestEnabledDocument(String docType) {
        return legalDocumentMapper.selectOne(new LambdaQueryWrapper<LegalDocument>()
                .eq(LegalDocument::getDocType, docType)
                .eq(LegalDocument::getEnabled, true)
                .orderByDesc(LegalDocument::getEffectiveDate)
                .orderByDesc(LegalDocument::getId)
                .last("LIMIT 1"));
    }

    @Override
    public String login(AuthLoginDTO dto) {
        AppUser user = appUserMapper.selectOne(
                new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, dto.getUsername()));

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        return jwtUtils.generateToken(user.getUsername(), user.getId());
    }
}
