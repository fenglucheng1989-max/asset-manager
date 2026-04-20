package com.yourcompany.assetmanager.controller;

import com.yourcompany.assetmanager.dto.AssetAccountDTO;
import com.yourcompany.assetmanager.dto.SortDTO;
import com.yourcompany.assetmanager.entity.AssetAccount;
import com.yourcompany.assetmanager.service.AssetAccountService;
import com.yourcompany.assetmanager.service.AssetOverviewService;
import com.yourcompany.assetmanager.vo.ApiResponse;
import com.yourcompany.assetmanager.vo.AssetOverviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetOverviewService assetOverviewService;
    private final AssetAccountService assetAccountService;

    @GetMapping("/overview")
    public ApiResponse<AssetOverviewVO> getOverview(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ApiResponse.success(assetOverviewService.getOverview(userId));
    }

    @GetMapping("/accounts")
    public ApiResponse<List<AssetAccount>> getAccounts(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ApiResponse.success(assetAccountService.getAccounts(userId));
    }

    @PostMapping("/accounts")
    public ApiResponse<AssetAccount> createAccount(Authentication authentication,
                                                   @Valid @RequestBody AssetAccountDTO dto) {
        Long userId = getCurrentUserId(authentication);
        return ApiResponse.success(assetAccountService.createAccount(userId, dto));
    }

    @PutMapping("/accounts/{id}")
    public ApiResponse<AssetAccount> updateAccount(Authentication authentication,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody AssetAccountDTO dto) {
        Long userId = getCurrentUserId(authentication);
        return ApiResponse.success(assetAccountService.updateAccount(userId, id, dto));
    }

    @DeleteMapping("/accounts/{id}")
    public ApiResponse<Void> deleteAccount(Authentication authentication,
                                           @PathVariable Long id) {
        Long userId = getCurrentUserId(authentication);
        assetAccountService.deleteAccount(userId, id);
        return ApiResponse.success(null);
    }

    @PutMapping("/accounts/sort")
    public ApiResponse<Void> updateSort(Authentication authentication,
                                        @Valid @RequestBody SortDTO sortDTO) {
        Long userId = getCurrentUserId(authentication);
        assetAccountService.updateSort(userId, sortDTO.getSortedIds());
        return ApiResponse.success(null);
    }

    private Long getCurrentUserId(Authentication authentication) {
        // The userId is stored in the JWT claims; we need a helper to extract it
        // For now, we retrieve the user from database by username
        String username = authentication.getName();
        // This is a simplified approach - in production, extract userId from JWT directly
        return getUserIdByUsername(username);
    }

    private Long getUserIdByUsername(String username) {
        // This will be resolved via the security context
        // We inject the AppUserMapper for this lookup
        return appUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.yourcompany.assetmanager.entity.AppUser>()
                        .eq(com.yourcompany.assetmanager.entity.AppUser::getUsername, username)).getId();
    }

    @jakarta.annotation.Resource
    private com.yourcompany.assetmanager.mapper.AppUserMapper appUserMapper;
}
