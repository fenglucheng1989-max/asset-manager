package com.yourcompany.assetmanager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yourcompany.assetmanager.entity.AppUser;
import com.yourcompany.assetmanager.entity.AssetSnapshot;
import com.yourcompany.assetmanager.mapper.AppUserMapper;
import com.yourcompany.assetmanager.mapper.AssetSnapshotMapper;
import com.yourcompany.assetmanager.service.AssetOverviewService;
import com.yourcompany.assetmanager.service.AssetSnapshotService;
import com.yourcompany.assetmanager.vo.AssetOverviewVO;
import com.yourcompany.assetmanager.vo.AssetSnapshotVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetSnapshotServiceImpl implements AssetSnapshotService {

    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_LIMIT = 365;

    private final AssetSnapshotMapper assetSnapshotMapper;
    private final AssetOverviewService assetOverviewService;
    private final AppUserMapper appUserMapper;

    @Override
    public AssetSnapshotVO createTodaySnapshot(Long userId) {
        LocalDate today = LocalDate.now();
        AssetOverviewVO overview = assetOverviewService.getOverview(userId);
        AssetSnapshot existing = assetSnapshotMapper.selectOne(
                new LambdaQueryWrapper<AssetSnapshot>()
                        .eq(AssetSnapshot::getUserId, userId)
                        .eq(AssetSnapshot::getSnapshotDate, today));

        if (existing == null) {
            AssetSnapshot snapshot = AssetSnapshot.builder()
                    .userId(userId)
                    .snapshotDate(today)
                    .totalAsset(overview.getTotalAsset())
                    .totalLiability(overview.getTotalLiability())
                    .netWorth(overview.getNetWorth())
                    .build();
            assetSnapshotMapper.insert(snapshot);
            return toVO(snapshot);
        }

        existing.setTotalAsset(overview.getTotalAsset());
        existing.setTotalLiability(overview.getTotalLiability());
        existing.setNetWorth(overview.getNetWorth());
        assetSnapshotMapper.updateById(existing);
        return toVO(existing);
    }

    @Override
    public List<AssetSnapshotVO> listSnapshots(Long userId, Integer limit) {
        int safeLimit = normalizeLimit(limit);
        List<AssetSnapshot> snapshots = assetSnapshotMapper.selectList(
                new LambdaQueryWrapper<AssetSnapshot>()
                        .eq(AssetSnapshot::getUserId, userId)
                        .orderByDesc(AssetSnapshot::getSnapshotDate)
                        .last("LIMIT " + safeLimit));
        Collections.reverse(snapshots);
        return snapshots.stream().map(this::toVO).toList();
    }

    @Override
    @Scheduled(cron = "${asset.snapshot.daily-cron:0 55 23 * * *}")
    public void createDailySnapshotsForAllUsers() {
        List<AppUser> users = appUserMapper.selectList(null);
        for (AppUser user : users) {
            try {
                createTodaySnapshot(user.getId());
            } catch (RuntimeException e) {
                log.warn("Create daily asset snapshot failed, userId={}", user.getId(), e);
            }
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        return Math.max(1, Math.min(MAX_LIMIT, limit));
    }

    private AssetSnapshotVO toVO(AssetSnapshot snapshot) {
        return AssetSnapshotVO.builder()
                .id(snapshot.getId())
                .snapshotDate(snapshot.getSnapshotDate())
                .totalAsset(snapshot.getTotalAsset())
                .totalLiability(snapshot.getTotalLiability())
                .netWorth(snapshot.getNetWorth())
                .build();
    }
}
