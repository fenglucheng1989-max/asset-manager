package com.yourcompany.assetmanager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yourcompany.assetmanager.dto.AuthLoginDTO;
import com.yourcompany.assetmanager.dto.AuthRegisterDTO;
import com.yourcompany.assetmanager.dto.AuthResult;
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
    public AuthResult register(AuthRegisterDTO dto) {
        ensureLegalAccepted(dto);

        String email = dto.getEmail().trim().toLowerCase();
        AppUser emailExists = appUserMapper.selectOne(
                new LambdaQueryWrapper<AppUser>().eq(AppUser::getEmail, email));
        if (emailExists != null) {
            throw new BusinessException("该邮箱已被注册");
        }

        String username = (dto.getUsername() != null && !dto.getUsername().isBlank())
                ? dto.getUsername().trim()
                : generateUsernameFromEmail(email);
        username = resolveUsernameCollision(username);

        AppUser user = AppUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .email(email)
                .role("USER")
                .acceptedTermsVersion(dto.getAcceptedTermsVersion())
                .acceptedPrivacyVersion(dto.getAcceptedPrivacyVersion())
                .legalAcceptedAt(LocalDateTime.now())
                .build();

        appUserMapper.insert(user);

        return new AuthResult(
                jwtUtils.generateToken(user.getEmail(), user.getId()),
                user.getUsername(),
                user.getEmail());
    }

    private String generateUsernameFromEmail(String email) {
        String prefix = email.substring(0, email.indexOf('@'));
        String cleaned = prefix.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "");
        return cleaned.isEmpty()
                ? "user_" + System.currentTimeMillis() % 100000
                : cleaned;
    }

    private String resolveUsernameCollision(String baseUsername) {
        String candidate = baseUsername;
        int suffix = 1;
        while (appUserMapper.selectOne(
                new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, candidate)) != null) {
            candidate = baseUsername + suffix;
            suffix++;
        }
        return candidate;
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
    public AuthResult login(AuthLoginDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();
        AppUser user = appUserMapper.selectOne(
                new LambdaQueryWrapper<AppUser>().eq(AppUser::getEmail, email));

        if (user == null) {
            user = appUserMapper.selectOne(
                    new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, dto.getEmail().trim()));
        }

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "邮箱或密码错误");
        }

        return new AuthResult(
                jwtUtils.generateToken(user.getEmail(), user.getId()),
                user.getUsername(),
                user.getEmail());
    }
}
