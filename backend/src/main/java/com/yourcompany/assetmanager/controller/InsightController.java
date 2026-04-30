package com.yourcompany.assetmanager.controller;

import com.yourcompany.assetmanager.dto.AssetMilestoneDTO;
import com.yourcompany.assetmanager.mapper.AppUserMapper;
import com.yourcompany.assetmanager.service.InsightService;
import com.yourcompany.assetmanager.vo.ApiResponse;
import com.yourcompany.assetmanager.vo.AssetMilestoneVO;
import com.yourcompany.assetmanager.vo.HealthScoreVO;
import com.yourcompany.assetmanager.vo.ReportSummaryVO;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/insight")
public class InsightController extends BaseUserController {

    private final InsightService insightService;

    public InsightController(AppUserMapper appUserMapper, InsightService insightService) {
        super(appUserMapper);
        this.insightService = insightService;
    }

    @GetMapping("/health-score")
    public ApiResponse<HealthScoreVO> healthScore(Authentication authentication) {
        return ApiResponse.success(insightService.healthScore(currentUserId(authentication)));
    }

    @GetMapping("/summaries")
    public ApiResponse<List<ReportSummaryVO>> summaries(Authentication authentication,
                                                        @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(insightService.summaries(currentUserId(authentication), limit));
    }

    @GetMapping("/milestones")
    public ApiResponse<List<AssetMilestoneVO>> milestones(Authentication authentication) {
        return ApiResponse.success(insightService.milestones(currentUserId(authentication)));
    }

    @PostMapping("/milestones")
    public ApiResponse<AssetMilestoneVO> createMilestone(Authentication authentication,
                                                        @Valid @RequestBody AssetMilestoneDTO dto) {
        return ApiResponse.success(insightService.createCustomMilestone(currentUserId(authentication), dto));
    }
}
