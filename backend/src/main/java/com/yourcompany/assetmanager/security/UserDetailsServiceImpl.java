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

import java.util.Collections;

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

        return new User(appUser.getUsername(), appUser.getPasswordHash(), Collections.emptyList());
    }
}
