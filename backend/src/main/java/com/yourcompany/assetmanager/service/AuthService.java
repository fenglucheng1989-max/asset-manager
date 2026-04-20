package com.yourcompany.assetmanager.service;

import com.yourcompany.assetmanager.dto.AuthLoginDTO;
import com.yourcompany.assetmanager.dto.AuthRegisterDTO;

public interface AuthService {

    String register(AuthRegisterDTO dto);

    String login(AuthLoginDTO dto);
}
