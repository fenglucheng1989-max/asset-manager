package com.yourcompany.assetmanager.controller;

import com.yourcompany.assetmanager.dto.TransactionBudgetDTO;
import com.yourcompany.assetmanager.dto.TransactionCategoryDTO;
import com.yourcompany.assetmanager.dto.TransactionRecordDTO;
import com.yourcompany.assetmanager.mapper.AppUserMapper;
import com.yourcompany.assetmanager.service.TransactionService;
import com.yourcompany.assetmanager.vo.ApiResponse;
import com.yourcompany.assetmanager.vo.TransactionBudgetVO;
import com.yourcompany.assetmanager.vo.TransactionCategoryVO;
import com.yourcompany.assetmanager.vo.TransactionRecordVO;
import com.yourcompany.assetmanager.vo.TransactionReportVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController extends BaseUserController {

    private final TransactionService transactionService;

    public TransactionController(AppUserMapper appUserMapper, TransactionService transactionService) {
        super(appUserMapper);
        this.transactionService = transactionService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<TransactionCategoryVO>> categories(Authentication authentication,
                                                              @RequestParam(required = false) String type) {
        Long userId = currentUserId(authentication);
        return ApiResponse.success(transactionService.listCategories(userId, type));
    }

    @PostMapping("/categories")
    public ApiResponse<TransactionCategoryVO> createCategory(Authentication authentication,
                                                            @Valid @RequestBody TransactionCategoryDTO dto) {
        Long userId = currentUserId(authentication);
        return ApiResponse.success(transactionService.createCategory(userId, dto));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<TransactionCategoryVO> updateCategory(Authentication authentication,
                                                            @PathVariable Long id,
                                                            @Valid @RequestBody TransactionCategoryDTO dto) {
        Long userId = currentUserId(authentication);
        return ApiResponse.success(transactionService.updateCategory(userId, id, dto));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(Authentication authentication, @PathVariable Long id) {
        Long userId = currentUserId(authentication);
        transactionService.deleteCategory(userId, id);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<TransactionRecordVO>> records(Authentication authentication,
                                                         @RequestParam(required = false) Integer limit,
                                                         @RequestParam(required = false) String type,
                                                         @RequestParam(required = false) Long accountId,
                                                         @RequestParam(required = false) Long categoryId,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = currentUserId(authentication);
        return ApiResponse.success(transactionService.listRecords(userId, limit, type, accountId, categoryId, startDate, endDate));
    }

    @PostMapping
    public ApiResponse<TransactionRecordVO> create(Authentication authentication,
                                                  @Valid @RequestBody TransactionRecordDTO dto) {
        Long userId = currentUserId(authentication);
        return ApiResponse.success(transactionService.createRecord(userId, dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<TransactionRecordVO> update(Authentication authentication,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody TransactionRecordDTO dto) {
        Long userId = currentUserId(authentication);
        return ApiResponse.success(transactionService.updateRecord(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        Long userId = currentUserId(authentication);
        transactionService.deleteRecord(userId, id);
        return ApiResponse.success(null);
    }

    @GetMapping("/budgets")
    public ApiResponse<List<TransactionBudgetVO>> budgets(Authentication authentication,
                                                         @RequestParam(required = false) String periodType,
                                                         @RequestParam(required = false) String periodKey,
                                                         @RequestParam(required = false) String month) {
        Long userId = currentUserId(authentication);
        return ApiResponse.success(transactionService.listBudgets(userId, periodType, periodKey, month));
    }

    @PostMapping("/budgets")
    public ApiResponse<TransactionBudgetVO> saveBudget(Authentication authentication,
                                                       @Valid @RequestBody TransactionBudgetDTO dto) {
        Long userId = currentUserId(authentication);
        return ApiResponse.success(transactionService.saveBudget(userId, dto));
    }

    @DeleteMapping("/budgets/{id}")
    public ApiResponse<Void> deleteBudget(Authentication authentication, @PathVariable Long id) {
        Long userId = currentUserId(authentication);
        transactionService.deleteBudget(userId, id);
        return ApiResponse.success(null);
    }

    @GetMapping("/report")
    public ApiResponse<TransactionReportVO> report(Authentication authentication,
                                                   @RequestParam(required = false) String month) {
        Long userId = currentUserId(authentication);
        return ApiResponse.success(transactionService.report(userId, month));
    }
}
