package com.crm.service;

import com.crm.dto.UnifiedLoginRequest;
import com.crm.dto.UnifiedLoginResponse;

public interface UnifiedAuthService {
    UnifiedLoginResponse authenticate(UnifiedLoginRequest loginRequest);
}