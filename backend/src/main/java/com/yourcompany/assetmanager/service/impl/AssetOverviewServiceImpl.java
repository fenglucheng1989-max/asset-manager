package com.yourcompany.assetmanager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yourcompany.assetmanager.entity.AssetAccount;
import com.yourcompany.assetmanager.mapper.AssetAccountMapper;
import com.yourcompany.assetmanager.service.AssetOverviewService;
import com.yourcompany.assetmanager.utils.MoneyUtils;
import com.yourcompany.assetmanager.vo.AssetOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AssetOverviewServiceImpl implements AssetOverviewService {

    private final AssetAccountMapper assetAccountMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "asset:overview:";
    private static final long CACHE_EXPIRE_MINUTES = 30;

    @Override
    public AssetOverviewVO getOverview(Long userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof AssetOverviewVO) {
            return (AssetOverviewVO) cached;
        }

        List<AssetAccount> accounts = assetAccountMapper.selectList(
                new LambdaQueryWrapper<AssetAccount>()
                        .eq(AssetAccount::getUserId, userId)
                        .eq(AssetAccount::getIncludeInTotal, true));

        BigDecimal totalAsset = BigDecimal.ZERO;
        BigDecimal totalLiability = BigDecimal.ZERO;
        LocalDateTime lastUpdate = null;

        for (AssetAccount account : accounts) {
            if (Boolean.TRUE.equals(account.getIsLiability())) {
                totalLiability = MoneyUtils.add(totalLiability, account.getCurrentBalance());
            } else {
                totalAsset = MoneyUtils.add(totalAsset, account.getCurrentBalance());
            }
            if (lastUpdate == null || (account.getUpdatedAt() != null && account.getUpdatedAt().isAfter(lastUpdate))) {
                lastUpdate = account.getUpdatedAt();
            }
        }

        BigDecimal netWorth = MoneyUtils.subtract(totalAsset, totalLiability);

        int totalAccountCount = Math.toIntExact(assetAccountMapper.selectCount(
                new LambdaQueryWrapper<AssetAccount>().eq(AssetAccount::getUserId, userId)));

        AssetOverviewVO overview = AssetOverviewVO.builder()
                .totalAsset(totalAsset)
                .totalLiability(totalLiability)
                .netWorth(netWorth)
                .accountCount(totalAccountCount)
                .lastUpdateTime(lastUpdate)
                .build();

        redisTemplate.opsForValue().set(cacheKey, overview, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        return overview;
    }

    @Override
    public void evictOverviewCache(Long userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;
        redisTemplate.delete(cacheKey);
    }
}
