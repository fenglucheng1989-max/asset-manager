package com.yourcompany.assetmanager.controller;

import com.yourcompany.assetmanager.dto.AssetAccountDTO;
import com.yourcompany.assetmanager.dto.SortDTO;
import com.yourcompany.assetmanager.entity.AssetAccount;
import com.yourcompany.assetmanager.service.AssetAccountService;
import com.yourcompany.assetmanager.service.AssetOverviewService;
import com.yourcompany.assetmanager.service.AssetSnapshotService;
import com.yourcompany.assetmanager.vo.ApiResponse;
import com.yourcompany.assetmanager.vo.AssetAccountBalanceHistoryVO;
import com.yourcompany.assetmanager.vo.AssetOverviewVO;
import com.yourcompany.assetmanager.vo.AssetSnapshotVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetOverviewService assetOverviewService;
    private final AssetAccountService assetAccountService;
    private final AssetSnapshotService assetSnapshotService;

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

    @GetMapping("/accounts/{id}")
    public ApiResponse<AssetAccount> getAccount(Authentication authentication, @PathVariable Long id) {
        Long userId = getCurrentUserId(authentication);
        return ApiResponse.success(assetAccountService.getAccount(userId, id));
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

    @PutMapping("/accounts/{id}/archive")
    public ApiResponse<AssetAccount> archiveAccount(Authentication authentication,
                                                    @PathVariable Long id,
                                                    @RequestBody(required = false) Map<String, Boolean> body) {
        Long userId = getCurrentUserId(authentication);
        boolean archived = body == null || body.get("archived") == null || Boolean.TRUE.equals(body.get("archived"));
        return ApiResponse.success(assetAccountService.archiveAccount(userId, id, archived));
    }

    @GetMapping("/accounts/{id}/history")
    public ApiResponse<List<AssetAccountBalanceHistoryVO>> getAccountHistory(Authentication authentication,
                                                                            @PathVariable Long id,
                                                                            @RequestParam(required = false) Integer limit) {
        Long userId = getCurrentUserId(authentication);
        return ApiResponse.success(assetAccountService.getBalanceHistory(userId, id, limit));
    }

    @PostMapping("/snapshots")
    public ApiResponse<AssetSnapshotVO> createSnapshot(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ApiResponse.success(assetSnapshotService.createTodaySnapshot(userId));
    }

    @GetMapping("/snapshots")
    public ApiResponse<List<AssetSnapshotVO>> listSnapshots(Authentication authentication,
                                                            @RequestParam(required = false) Integer limit,
                                                            @RequestParam(required = false) Integer offset) {
        Long userId = getCurrentUserId(authentication);
        return ApiResponse.success(assetSnapshotService.listSnapshots(userId, limit, offset));
    }

    private Long getCurrentUserId(Authentication authentication) {
        String email = authentication.getName();
        return getUserIdByEmail(email);
    }

    private Long getUserIdByEmail(String email) {
        return appUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.yourcompany.assetmanager.entity.AppUser>()
                        .eq(com.yourcompany.assetmanager.entity.AppUser::getEmail, email)).getId();
    }

    @jakarta.annotation.Resource
    private com.yourcompany.assetmanager.mapper.AppUserMapper appUserMapper;
}
