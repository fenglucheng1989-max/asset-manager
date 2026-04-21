package com.yourcompany.assetmanager.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yourcompany.assetmanager.entity.AppUser;
import com.yourcompany.assetmanager.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserMapper appUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = appUserMapper.selectOne(
                new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, username));

        if (appUser == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        String role = appUser.getRole() == null || appUser.getRole().isBlank() ? "USER" : appUser.getRole();
        return User.withUsername(appUser.getUsername())
                .password(appUser.getPasswordHash())
                .authorities(List.of(() -> "ROLE_" + role.toUpperCase()))
                .build();
    }
}
