package com.yourcompany.assetmanager.service;

import com.yourcompany.assetmanager.dto.TransactionRecordDTO;
import com.yourcompany.assetmanager.dto.TransactionBudgetDTO;
import com.yourcompany.assetmanager.dto.TransactionCategoryDTO;
import com.yourcompany.assetmanager.vo.TransactionBudgetVO;
import com.yourcompany.assetmanager.vo.TransactionCategoryVO;
import com.yourcompany.assetmanager.vo.TransactionRecordVO;
import com.yourcompany.assetmanager.vo.TransactionReportVO;

import java.time.LocalDate;

import java.util.List;

public interface TransactionService {

    List<TransactionCategoryVO> listCategories(Long userId, String type);

    TransactionCategoryVO createCategory(Long userId, TransactionCategoryDTO dto);

    TransactionCategoryVO updateCategory(Long userId, Long categoryId, TransactionCategoryDTO dto);

    void deleteCategory(Long userId, Long categoryId);

    List<TransactionRecordVO> listRecords(Long userId, Integer limit, String type, Long accountId, Long categoryId, LocalDate startDate, LocalDate endDate);

    TransactionRecordVO createRecord(Long userId, TransactionRecordDTO dto);

    TransactionRecordVO updateRecord(Long userId, Long recordId, TransactionRecordDTO dto);

    void deleteRecord(Long userId, Long recordId);

    List<TransactionBudgetVO> listBudgets(Long userId, String periodType, String periodKey, String month);

    TransactionBudgetVO saveBudget(Long userId, TransactionBudgetDTO dto);

    void deleteBudget(Long userId, Long budgetId);

    TransactionReportVO report(Long userId, String month);
}
