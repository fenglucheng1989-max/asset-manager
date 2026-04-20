package com.yourcompany.assetmanager.service;

import com.yourcompany.assetmanager.vo.AssetOverviewVO;

public interface AssetOverviewService {

    AssetOverviewVO getOverview(Long userId);

    void evictOverviewCache(Long userId);
}
