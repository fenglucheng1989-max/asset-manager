package com.yourcompany.assetmanager.service;

import com.yourcompany.assetmanager.entity.AppUser;

import java.util.Map;

public interface UserAccountCenterService {

    String exportCsv(Long userId, String type);

    Map<String, Object> exportBackup(Long userId);

    void restoreBackup(Long userId, Map<String, Object> backup);

    void clearUserData(Long userId);

    void changePassword(AppUser user, String currentPassword, String newPassword);

    void deleteAccount(AppUser user, String currentPassword);
}
