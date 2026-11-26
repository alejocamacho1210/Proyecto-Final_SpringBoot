package com.coworkhub.business;

import com.coworkhub.presentation.ApiDtos.LoginResponse;
import com.coworkhub.presentation.ApiDtos.MemberRequest;

public interface AuthService {
    LoginResponse login(String username, String password);
    LoginResponse register(MemberRequest request);
}
