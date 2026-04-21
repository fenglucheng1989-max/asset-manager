package com.yourcompany.assetmanager.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yourcompany.assetmanager.entity.AppUser;
import com.yourcompany.assetmanager.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalAdminInitializer implements CommandLineRunner {

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        AppUser existing = appUserMapper.selectOne(
                new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, "admin"));
        if (existing != null) {
            if (!"ADMIN".equalsIgnoreCase(existing.getRole())) {
                existing.setRole("ADMIN");
                appUserMapper.updateById(existing);
            }
            return;
        }

        AppUser admin = AppUser.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("admin123456"))
                .email("admin@local")
                .role("ADMIN")
                .build();
        appUserMapper.insert(admin);
    }
}
