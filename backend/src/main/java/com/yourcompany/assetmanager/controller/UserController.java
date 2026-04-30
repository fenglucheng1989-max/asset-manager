package com.yourcompany.assetmanager.controller;

import com.yourcompany.assetmanager.dto.ChangePasswordDTO;
import com.yourcompany.assetmanager.dto.DeleteAccountDTO;
import com.yourcompany.assetmanager.dto.UpdateProfileDTO;
import com.yourcompany.assetmanager.dto.UserFeedbackDTO;
import com.yourcompany.assetmanager.entity.AppUser;
import com.yourcompany.assetmanager.entity.UserFeedback;
import com.yourcompany.assetmanager.exception.BusinessException;
import com.yourcompany.assetmanager.mapper.AppUserMapper;
import com.yourcompany.assetmanager.mapper.UserFeedbackMapper;
import com.yourcompany.assetmanager.service.UserAccountCenterService;
import com.yourcompany.assetmanager.vo.ApiResponse;
import com.yourcompany.assetmanager.vo.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/user")
public class UserController extends BaseUserController {

    private final AppUserMapper appUserMapper;
    private final UserFeedbackMapper userFeedbackMapper;
    private final UserAccountCenterService accountCenterService;

    public UserController(AppUserMapper appUserMapper,
                          UserFeedbackMapper userFeedbackMapper,
                          UserAccountCenterService accountCenterService) {
        super(appUserMapper);
        this.appUserMapper = appUserMapper;
        this.userFeedbackMapper = userFeedbackMapper;
        this.accountCenterService = accountCenterService;
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

    @PutMapping("/profile")
    public ApiResponse<UserProfileVO> updateProfile(Authentication authentication,
                                                    @RequestBody UpdateProfileDTO dto) {
        AppUser user = currentUser(authentication);
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl().trim());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail().trim().isEmpty() ? null : dto.getEmail().trim());
        }
        appUserMapper.updateById(user);
        return ApiResponse.success(toProfile(user));
    }

    @GetMapping("/data/export/{type}")
    public ResponseEntity<String> exportCsv(Authentication authentication, @PathVariable String type) {
        Long userId = currentUserId(authentication);
        String csv = accountCenterService.exportCsv(userId, type);
        String filename = "asset-manager-" + type + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    @GetMapping("/data/backup")
    public ApiResponse<Map<String, Object>> exportBackup(Authentication authentication) {
        return ApiResponse.success(accountCenterService.exportBackup(currentUserId(authentication)));
    }

    @PostMapping("/data/restore")
    public ApiResponse<Void> restoreBackup(Authentication authentication,
                                           @RequestBody Map<String, Object> backup) {
        accountCenterService.restoreBackup(currentUserId(authentication), backup);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/data")
    public ApiResponse<Void> clearData(Authentication authentication) {
        accountCenterService.clearUserData(currentUserId(authentication));
        return ApiResponse.success(null);
    }

    @PutMapping("/security/password")
    public ApiResponse<Void> changePassword(Authentication authentication,
                                            @Valid @RequestBody ChangePasswordDTO dto) {
        accountCenterService.changePassword(currentUser(authentication), dto.getCurrentPassword(), dto.getNewPassword());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/account")
    public ApiResponse<Void> deleteAccount(Authentication authentication,
                                           @Valid @RequestBody DeleteAccountDTO dto) {
        accountCenterService.deleteAccount(currentUser(authentication), dto.getCurrentPassword());
        return ApiResponse.success(null);
    }

    @PostMapping("/feedback")
    public ApiResponse<Void> submitFeedback(Authentication authentication,
                                            @Valid @RequestBody UserFeedbackDTO dto) {
        UserFeedback feedback = UserFeedback.builder()
                .userId(currentUserId(authentication))
                .type(normalizeFeedbackType(dto.getType()))
                .content(dto.getContent().trim())
                .contact(dto.getContact() == null ? null : dto.getContact().trim())
                .status("OPEN")
                .build();
        userFeedbackMapper.insert(feedback);
        return ApiResponse.success(null);
    }

    private String normalizeFeedbackType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase();
        return switch (normalized) {
            case "BUG", "SUGGESTION", "EXPORT", "ACCOUNT", "OTHER" -> normalized;
            default -> "OTHER";
        };
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
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() == null ? "USER" : user.getRole())
                .baseCurrency(user.getBaseCurrency() == null || user.getBaseCurrency().isBlank()
                        ? "CNY"
                        : user.getBaseCurrency())
                .createdAt(user.getCreatedAt() == null ? null : user.getCreatedAt().toString())
                .build();
    }
}
