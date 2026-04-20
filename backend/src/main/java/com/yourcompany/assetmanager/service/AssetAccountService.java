package com.yourcompany.assetmanager.service;

import com.yourcompany.assetmanager.dto.AssetAccountDTO;
import com.yourcompany.assetmanager.entity.AssetAccount;

import java.util.List;

public interface AssetAccountService {

    AssetAccount createAccount(Long userId, AssetAccountDTO dto);

    AssetAccount updateAccount(Long userId, Long accountId, AssetAccountDTO dto);

    void deleteAccount(Long userId, Long accountId);

    List<AssetAccount> getAccounts(Long userId);

    void updateSort(Long userId, List<Long> sortedIds);
}
