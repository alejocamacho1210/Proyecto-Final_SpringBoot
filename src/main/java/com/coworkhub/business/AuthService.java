package com.coworkhub.business;

import com.coworkhub.presentation.ApiDtos.LoginResponse;

public interface AuthService {
    LoginResponse login(String username, String password);
}
