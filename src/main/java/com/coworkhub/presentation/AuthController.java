package com.coworkhub.presentation;

import com.coworkhub.business.AuthService;
import com.coworkhub.presentation.ApiDtos.*;
import com.coworkhub.security.AccountUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    @SecurityRequirements
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.username(), request.password());
    }

    @GetMapping("/perfil")
    @SecurityRequirement(name = "bearerAuth")
    public ProfileResponse profile(@AuthenticationPrincipal AccountUserDetails user) {
        return new ProfileResponse(user.getUsername(), user.account().getRole(), user.memberId());
    }
}
