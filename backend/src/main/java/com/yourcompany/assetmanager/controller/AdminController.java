package com.yourcompany.assetmanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yourcompany.assetmanager.dto.AssetAccountDTO;
import com.yourcompany.assetmanager.entity.AppUser;
import com.yourcompany.assetmanager.entity.AssetAccount;
import com.yourcompany.assetmanager.exception.BusinessException;
import com.yourcompany.assetmanager.mapper.AppUserMapper;
import com.yourcompany.assetmanager.mapper.AssetAccountMapper;
import com.yourcompany.assetmanager.service.AssetOverviewService;
import com.yourcompany.assetmanager.utils.MoneyUtils;
import com.yourcompany.assetmanager.vo.AdminAccountVO;
import com.yourcompany.assetmanager.vo.AdminDashboardVO;
import com.yourcompany.assetmanager.vo.AdminUserVO;
import com.yourcompany.assetmanager.vo.ApiResponse;
import com.yourcompany.assetmanager.vo.AssetOverviewVO;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController extends BaseUserController {

    private final AppUserMapper appUserMapper;
    private final AssetAccountMapper assetAccountMapper;
    private final AssetOverviewService assetOverviewService;

    public AdminController(AppUserMapper appUserMapper,
                           AssetAccountMapper assetAccountMapper,
                           AssetOverviewService assetOverviewService) {
        super(appUserMapper);
        this.appUserMapper = appUserMapper;
        this.assetAccountMapper = assetAccountMapper;
        this.assetOverviewService = assetOverviewService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardVO> dashboard(Authentication authentication) {
        requireAdmin(authentication);
        List<AssetAccount> includedAccounts = assetAccountMapper.selectList(
                new LambdaQueryWrapper<AssetAccount>().eq(AssetAccount::getIncludeInTotal, true));

        BigDecimal totalAsset = BigDecimal.ZERO;
        BigDecimal totalLiability = BigDecimal.ZERO;
        long assetAccountCount = 0;
        long liabilityAccountCount = 0;
        for (AssetAccount account : includedAccounts) {
            if (Boolean.TRUE.equals(account.getIsLiability())) {
                liabilityAccountCount++;
                totalLiability = MoneyUtils.add(totalLiability, account.getCurrentBalance());
            } else {
                assetAccountCount++;
                totalAsset = MoneyUtils.add(totalAsset, account.getCurrentBalance());
            }
        }

        return ApiResponse.success(AdminDashboardVO.builder()
                .userCount(appUserMapper.selectCount(null))
                .accountCount(assetAccountMapper.selectCount(null))
                .assetAccountCount(assetAccountCount)
                .liabilityAccountCount(liabilityAccountCount)
                .totalAsset(totalAsset)
                .totalLiability(totalLiability)
                .netWorth(MoneyUtils.subtract(totalAsset, totalLiability))
                .build());
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserVO>> users(Authentication authentication,
                                               @RequestParam(required = false) String keyword) {
        requireAdmin(authentication);
        LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<AppUser>()
                .orderByDesc(AppUser::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(AppUser::getUsername, keyword).or().like(AppUser::getEmail, keyword));
        }

        List<AdminUserVO> users = appUserMapper.selectList(wrapper).stream()
                .map(this::toAdminUser)
                .collect(Collectors.toList());
        return ApiResponse.success(users);
    }

    @PutMapping("/users/{id}/role")
    public ApiResponse<AppUser> updateUserRole(Authentication authentication,
                                               @PathVariable Long id,
                                               @RequestBody Map<String, String> body) {
        requireAdmin(authentication);
        String role = body.getOrDefault("role", "USER").toUpperCase();
        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException(400, "Unsupported role");
        }

        AppUser user = appUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        user.setRole(role);
        appUserMapper.updateById(user);
        return ApiResponse.success(user);
    }

    @GetMapping("/accounts")
    public ApiResponse<List<AdminAccountVO>> accounts(Authentication authentication,
                                                     @RequestParam(required = false) Long userId,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String accountType) {
        requireAdmin(authentication);
        LambdaQueryWrapper<AssetAccount> wrapper = new LambdaQueryWrapper<AssetAccount>()
                .orderByDesc(AssetAccount::getUpdatedAt)
                .orderByDesc(AssetAccount::getId);
        if (userId != null) {
            wrapper.eq(AssetAccount::getUserId, userId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(AssetAccount::getName, keyword);
        }
        if (accountType != null && !accountType.isBlank()) {
            wrapper.eq(AssetAccount::getAccountType, accountType);
        }

        Map<Long, String> usernames = appUserMapper.selectList(null).stream()
                .collect(Collectors.toMap(AppUser::getId, AppUser::getUsername));
        List<AdminAccountVO> accounts = assetAccountMapper.selectList(wrapper).stream()
                .map(account -> toAdminAccount(account, usernames.get(account.getUserId())))
                .collect(Collectors.toList());
        return ApiResponse.success(accounts);
    }

    @PutMapping("/accounts/{id}")
    public ApiResponse<AdminAccountVO> updateAccount(Authentication authentication,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody AssetAccountDTO dto) {
        requireAdmin(authentication);
        AssetAccount account = assetAccountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(404, "Account not found");
        }

        account.setName(dto.getName());
        account.setAccountType(dto.getAccountType());
        account.setCurrency(dto.getCurrency());
        account.setCurrentBalance(dto.getCurrentBalance());
        account.setIsLiability(dto.getIsLiability());
        account.setIncludeInTotal(dto.getIncludeInTotal());
        account.setIcon(dto.getIcon());
        account.setColorHex(dto.getColorHex());
        account.setSortOrder(dto.getSortOrder());
        account.setRemark(dto.getRemark());
        assetAccountMapper.updateById(account);
        assetOverviewService.evictOverviewCache(account.getUserId());

        AppUser user = appUserMapper.selectById(account.getUserId());
        return ApiResponse.success(toAdminAccount(account, user == null ? null : user.getUsername()));
    }

    @DeleteMapping("/accounts/{id}")
    public ApiResponse<Void> deleteAccount(Authentication authentication, @PathVariable Long id) {
        requireAdmin(authentication);
        AssetAccount account = assetAccountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(404, "Account not found");
        }
        assetAccountMapper.deleteById(id);
        assetOverviewService.evictOverviewCache(account.getUserId());
        return ApiResponse.success(null);
    }

    private AdminUserVO toAdminUser(AppUser user) {
        AssetOverviewVO overview = assetOverviewService.getOverview(user.getId());
        return AdminUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole() == null ? "USER" : user.getRole())
                .accountCount((long) overview.getAccountCount())
                .totalAsset(overview.getTotalAsset())
                .totalLiability(overview.getTotalLiability())
                .netWorth(overview.getNetWorth())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AdminAccountVO toAdminAccount(AssetAccount account, String username) {
        return AdminAccountVO.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .username(Objects.requireNonNullElse(username, "-"))
                .name(account.getName())
                .accountType(account.getAccountType())
                .currency(account.getCurrency())
                .currentBalance(account.getCurrentBalance())
                .isLiability(account.getIsLiability())
                .includeInTotal(account.getIncludeInTotal())
                .colorHex(account.getColorHex())
                .sortOrder(account.getSortOrder())
                .remark(account.getRemark())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
