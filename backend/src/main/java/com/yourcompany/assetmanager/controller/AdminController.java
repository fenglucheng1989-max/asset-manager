package com.yourcompany.assetmanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yourcompany.assetmanager.dto.AssetAccountDTO;
import com.yourcompany.assetmanager.dto.LegalDocumentDTO;
import com.yourcompany.assetmanager.entity.AppUser;
import com.yourcompany.assetmanager.entity.AssetAccount;
import com.yourcompany.assetmanager.entity.LegalDocument;
import com.yourcompany.assetmanager.entity.TransactionCategory;
import com.yourcompany.assetmanager.entity.TransactionRecord;
import com.yourcompany.assetmanager.exception.BusinessException;
import com.yourcompany.assetmanager.mapper.AppUserMapper;
import com.yourcompany.assetmanager.mapper.AssetAccountMapper;
import com.yourcompany.assetmanager.mapper.LegalDocumentMapper;
import com.yourcompany.assetmanager.mapper.TransactionCategoryMapper;
import com.yourcompany.assetmanager.mapper.TransactionRecordMapper;
import com.yourcompany.assetmanager.service.AssetOverviewService;
import com.yourcompany.assetmanager.utils.MoneyUtils;
import com.yourcompany.assetmanager.vo.AdminAccountVO;
import com.yourcompany.assetmanager.vo.AdminDashboardVO;
import com.yourcompany.assetmanager.vo.AdminUserVO;
import com.yourcompany.assetmanager.vo.ApiResponse;
import com.yourcompany.assetmanager.vo.AssetOverviewVO;
import com.yourcompany.assetmanager.vo.LegalDocumentVO;
import com.yourcompany.assetmanager.vo.TransactionRecordVO;
import com.yourcompany.assetmanager.vo.TransactionReportVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController extends BaseUserController {

    private final AppUserMapper appUserMapper;
    private final AssetAccountMapper assetAccountMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final TransactionCategoryMapper transactionCategoryMapper;
    private final LegalDocumentMapper legalDocumentMapper;
    private final AssetOverviewService assetOverviewService;

    public AdminController(AppUserMapper appUserMapper,
                           AssetAccountMapper assetAccountMapper,
                           TransactionRecordMapper transactionRecordMapper,
                           TransactionCategoryMapper transactionCategoryMapper,
                           LegalDocumentMapper legalDocumentMapper,
                           AssetOverviewService assetOverviewService) {
        super(appUserMapper);
        this.appUserMapper = appUserMapper;
        this.assetAccountMapper = assetAccountMapper;
        this.transactionRecordMapper = transactionRecordMapper;
        this.transactionCategoryMapper = transactionCategoryMapper;
        this.legalDocumentMapper = legalDocumentMapper;
        this.assetOverviewService = assetOverviewService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardVO> dashboard(Authentication authentication) {
        requireAdmin(authentication);
        List<AssetAccount> includedAccounts = assetAccountMapper.selectList(
                new LambdaQueryWrapper<AssetAccount>()
                        .eq(AssetAccount::getIncludeInTotal, true)
                        .eq(AssetAccount::getArchived, false));

        BigDecimal totalAsset = BigDecimal.ZERO;
        BigDecimal totalLiability = BigDecimal.ZERO;
        long assetAccountCount = 0;
        long liabilityAccountCount = 0;
        for (AssetAccount account : includedAccounts) {
            if (Boolean.TRUE.equals(account.getIsLiability())) {
                liabilityAccountCount++;
                totalLiability = MoneyUtils.add(totalLiability,
                        MoneyUtils.toBaseCurrency(account.getCurrentBalance(), account.getExchangeRateToCny()));
            } else {
                assetAccountCount++;
                totalAsset = MoneyUtils.add(totalAsset,
                        MoneyUtils.toBaseCurrency(account.getCurrentBalance(), account.getExchangeRateToCny()));
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
            throw new BusinessException(400, "不支持的角色");
        }

        AppUser user = appUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
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
            throw new BusinessException(404, "账户不存在");
        }

        account.setName(dto.getName());
        account.setAccountType(dto.getAccountType());
        account.setCurrency(dto.getCurrency());
        account.setExchangeRateToCny(dto.getExchangeRateToCny());
        account.setCurrentBalance(dto.getCurrentBalance());
        account.setIsLiability(dto.getIsLiability());
        account.setIncludeInTotal(dto.getIncludeInTotal());
        account.setArchived(Boolean.TRUE.equals(dto.getArchived()));
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
            throw new BusinessException(404, "账户不存在");
        }
        assetAccountMapper.deleteById(id);
        assetOverviewService.evictOverviewCache(account.getUserId());
        return ApiResponse.success(null);
    }

    @GetMapping("/transactions")
    public ApiResponse<List<TransactionRecordVO>> transactions(Authentication authentication,
                                                              @RequestParam(required = false) Long userId,
                                                              @RequestParam(required = false) String type,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                              @RequestParam(required = false, defaultValue = "200") Integer limit) {
        requireAdmin(authentication);
        int safeLimit = Math.max(1, Math.min(500, limit));
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<TransactionRecord>()
                .orderByDesc(TransactionRecord::getOccurredAt)
                .orderByDesc(TransactionRecord::getId)
                .last("LIMIT " + safeLimit);
        if (userId != null) wrapper.eq(TransactionRecord::getUserId, userId);
        if (type != null && !type.isBlank()) wrapper.eq(TransactionRecord::getTransactionType, type.toUpperCase());
        if (startDate != null) wrapper.ge(TransactionRecord::getOccurredAt, startDate.atStartOfDay());
        if (endDate != null) wrapper.lt(TransactionRecord::getOccurredAt, endDate.plusDays(1).atStartOfDay());
        return ApiResponse.success(toTransactionVOs(transactionRecordMapper.selectList(wrapper)));
    }

    @GetMapping("/transactions/report")
    public ApiResponse<TransactionReportVO> transactionReport(Authentication authentication,
                                                             @RequestParam(required = false) String month) {
        requireAdmin(authentication);
        YearMonth ym = month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
        List<TransactionRecord> records = transactionRecordMapper.selectList(new LambdaQueryWrapper<TransactionRecord>()
                .ge(TransactionRecord::getOccurredAt, ym.atDay(1).atStartOfDay())
                .lt(TransactionRecord::getOccurredAt, ym.plusMonths(1).atDay(1).atStartOfDay())
                .orderByAsc(TransactionRecord::getOccurredAt));
        BigDecimal income = records.stream()
                .filter(record -> "INCOME".equals(record.getTransactionType()))
                .map(record -> MoneyUtils.toBaseCurrency(record.getAmount(), record.getExchangeRateToCny()))
                .reduce(BigDecimal.ZERO, MoneyUtils::add);
        BigDecimal expense = records.stream()
                .filter(record -> "EXPENSE".equals(record.getTransactionType()))
                .map(record -> MoneyUtils.toBaseCurrency(record.getAmount(), record.getExchangeRateToCny()))
                .reduce(BigDecimal.ZERO, MoneyUtils::add);
        return ApiResponse.success(TransactionReportVO.builder()
                .income(income)
                .expense(expense)
                .net(MoneyUtils.subtract(income, expense))
                .categoryStats(List.of())
                .trend(List.of())
                .build());
    }

    @GetMapping("/legal-documents")
    public ApiResponse<List<LegalDocumentVO>> legalDocuments(Authentication authentication,
                                                            @RequestParam(required = false) String type) {
        requireAdmin(authentication);
        LambdaQueryWrapper<LegalDocument> wrapper = new LambdaQueryWrapper<LegalDocument>()
                .orderByAsc(LegalDocument::getDocType)
                .orderByDesc(LegalDocument::getEffectiveDate)
                .orderByDesc(LegalDocument::getId);
        if (type != null && !type.isBlank()) {
            wrapper.eq(LegalDocument::getDocType, normalizeDocType(type));
        }
        return ApiResponse.success(legalDocumentMapper.selectList(wrapper).stream().map(this::toLegalDocumentVO).toList());
    }

    @PostMapping("/legal-documents")
    public ApiResponse<LegalDocumentVO> createLegalDocument(Authentication authentication,
                                                           @Valid @RequestBody LegalDocumentDTO dto) {
        requireAdmin(authentication);
        LegalDocument document = LegalDocument.builder()
                .docType(normalizeDocType(dto.getDocType()))
                .title(dto.getTitle().trim())
                .version(dto.getVersion().trim())
                .effectiveDate(dto.getEffectiveDate())
                .content(dto.getContent().trim())
                .enabled(Boolean.TRUE.equals(dto.getEnabled()))
                .build();
        legalDocumentMapper.insert(document);
        return ApiResponse.success(toLegalDocumentVO(document));
    }

    @PutMapping("/legal-documents/{id}")
    public ApiResponse<LegalDocumentVO> updateLegalDocument(Authentication authentication,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody LegalDocumentDTO dto) {
        requireAdmin(authentication);
        LegalDocument document = legalDocumentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException(404, "法律文档不存在");
        }
        document.setDocType(normalizeDocType(dto.getDocType()));
        document.setTitle(dto.getTitle().trim());
        document.setVersion(dto.getVersion().trim());
        document.setEffectiveDate(dto.getEffectiveDate());
        document.setContent(dto.getContent().trim());
        document.setEnabled(Boolean.TRUE.equals(dto.getEnabled()));
        legalDocumentMapper.updateById(document);
        return ApiResponse.success(toLegalDocumentVO(document));
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
                .exchangeRateToCny(account.getExchangeRateToCny())
                .currentBalance(account.getCurrentBalance())
                .isLiability(account.getIsLiability())
                .includeInTotal(account.getIncludeInTotal())
                .archived(account.getArchived())
                .colorHex(account.getColorHex())
                .sortOrder(account.getSortOrder())
                .remark(account.getRemark())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private List<TransactionRecordVO> toTransactionVOs(List<TransactionRecord> records) {
        Map<Long, AssetAccount> accounts = assetAccountMapper.selectList(null).stream()
                .collect(Collectors.toMap(AssetAccount::getId, Function.identity(), (a, b) -> a));
        Map<Long, TransactionCategory> categories = transactionCategoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(TransactionCategory::getId, Function.identity(), (a, b) -> a));
        return records.stream().map(record -> {
            AssetAccount account = accounts.get(record.getAccountId());
            AssetAccount targetAccount = accounts.get(record.getTargetAccountId());
            TransactionCategory category = categories.get(record.getCategoryId());
            return TransactionRecordVO.builder()
                    .id(record.getId())
                    .transactionType(record.getTransactionType())
                    .accountId(record.getAccountId())
                    .accountName(account == null ? null : account.getName())
                    .targetAccountId(record.getTargetAccountId())
                    .targetAccountName(targetAccount == null ? null : targetAccount.getName())
                    .categoryId(record.getCategoryId())
                    .categoryName(category == null ? null : category.getName())
                    .amount(record.getAmount())
                    .currency(record.getCurrency())
                    .exchangeRateToCny(record.getExchangeRateToCny())
                    .baseAmount(MoneyUtils.toBaseCurrency(record.getAmount(), record.getExchangeRateToCny()))
                    .occurredAt(record.getOccurredAt())
                    .tag(record.getTag())
                    .remark(record.getRemark())
                    .createdAt(record.getCreatedAt())
                    .build();
        }).toList();
    }

    private String normalizeDocType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase();
        return switch (normalized) {
            case "TERMS", "PRIVACY" -> normalized;
            default -> throw new BusinessException(400, "不支持的文档类型");
        };
    }

    private LegalDocumentVO toLegalDocumentVO(LegalDocument document) {
        return LegalDocumentVO.builder()
                .id(document.getId())
                .docType(document.getDocType())
                .title(document.getTitle())
                .version(document.getVersion())
                .effectiveDate(document.getEffectiveDate())
                .content(document.getContent())
                .enabled(document.getEnabled())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
