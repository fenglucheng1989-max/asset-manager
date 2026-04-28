package com.yourcompany.assetmanager.service;

import com.yourcompany.assetmanager.vo.AssetSnapshotVO;

import java.util.List;

public interface AssetSnapshotService {

    AssetSnapshotVO createTodaySnapshot(Long userId);

    List<AssetSnapshotVO> listSnapshots(Long userId, Integer limit, Integer offset);

    void createDailySnapshotsForAllUsers();
}
