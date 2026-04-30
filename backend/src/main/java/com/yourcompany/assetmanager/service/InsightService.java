package com.yourcompany.assetmanager.service;

import com.yourcompany.assetmanager.dto.AssetMilestoneDTO;
import com.yourcompany.assetmanager.vo.AssetMilestoneVO;
import com.yourcompany.assetmanager.vo.HealthScoreVO;
import com.yourcompany.assetmanager.vo.ReportSummaryVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InsightService {

    HealthScoreVO healthScore(Long userId);

    List<ReportSummaryVO> summaries(Long userId, Integer limit);

    ReportSummaryVO generateMonthlySummary(Long userId, LocalDate monthDate);

    List<AssetMilestoneVO> milestones(Long userId);

    AssetMilestoneVO createCustomMilestone(Long userId, AssetMilestoneDTO dto);

    void detectNetWorthMilestones(Long userId, BigDecimal netWorth, LocalDate achievedAt);
}
