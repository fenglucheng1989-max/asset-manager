package com.yourcompany.assetmanager.service;

import com.yourcompany.assetmanager.dto.AuthLoginDTO;
import com.yourcompany.assetmanager.dto.AuthRegisterDTO;
import com.yourcompany.assetmanager.dto.AuthResult;

public interface AuthService {

    AuthResult register(AuthRegisterDTO dto);

    AuthResult login(AuthLoginDTO dto);
}
