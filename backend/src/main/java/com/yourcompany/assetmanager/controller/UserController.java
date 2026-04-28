package com.yourcompany.assetmanager.controller;

import com.yourcompany.assetmanager.entity.AppUser;
import com.yourcompany.assetmanager.exception.BusinessException;
import com.yourcompany.assetmanager.mapper.AppUserMapper;
import com.yourcompany.assetmanager.vo.ApiResponse;
import com.yourcompany.assetmanager.vo.UserProfileVO;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class UserController extends BaseUserController {

    private final AppUserMapper appUserMapper;

    public UserController(AppUserMapper appUserMapper) {
        super(appUserMapper);
        this.appUserMapper = appUserMapper;
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileVO> profile(Authentication authentication) {
        return ApiResponse.success(toProfile(currentUser(authentication)));
    }

    @PutMapping("/profile/base-currency")
    public ApiResponse<UserProfileVO> updateBaseCurrency(Authentication authentication,
                                                         @RequestBody Map<String, String> body) {
        AppUser user = currentUser(authentication);
        String currency = normalizeCurrency(body == null ? null : body.get("baseCurrency"));
        user.setBaseCurrency(currency);
        appUserMapper.updateById(user);
        return ApiResponse.success(toProfile(user));
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) return "CNY";
        String normalized = currency.trim().toUpperCase();
        if (normalized.length() != 3) {
            throw new BusinessException(400, "本位币格式不正确");
        }
        return normalized;
    }

    private UserProfileVO toProfile(AppUser user) {
        return UserProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole() == null ? "USER" : user.getRole())
                .baseCurrency(user.getBaseCurrency() == null || user.getBaseCurrency().isBlank()
                        ? "CNY"
                        : user.getBaseCurrency())
                .build();
    }
}
