package com.coworkhub.business;

import com.coworkhub.security.AccountUserDetails;
import com.coworkhub.security.JwtService;
import com.coworkhub.presentation.ApiDtos.LoginResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager; this.jwtService = jwtService;
    }
    @Override
    public LoginResponse login(String username, String password) {
        try {
            var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
            String token = jwtService.createToken((AccountUserDetails) authentication.getPrincipal());
            return new LoginResponse(token, "Bearer", jwtService.expirationMinutes());
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new BusinessException("Usuario o contraseña incorrectos", "UNAUTHORIZED", org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
    }
}
