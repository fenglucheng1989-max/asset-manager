package com.yourcompany.assetmanager.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthScoreVO {

    private Integer score;

    private String grade;

    private String title;

    private String summary;

    private List<MetricVO> metrics;

    private Boolean dataInsufficient;

    private String dataNotice;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricVO {
        private String key;
        private String name;
        private BigDecimal value;
        private String unit;
        private String level;
        private String conclusion;
        private String suggestion;
    }
}
