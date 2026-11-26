package com.coworkhub.presentation;

import java.util.List;

import com.coworkhub.business.AuthService;
import com.coworkhub.business.CatalogService;
import com.coworkhub.presentation.ApiDtos.*;
import com.coworkhub.security.AccountUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;
    private final CatalogService catalogs;
    public AuthController(AuthService auth, CatalogService catalogs) {
        this.auth = auth;
        this.catalogs = catalogs;
    }

    @PostMapping("/login")
    @SecurityRequirements
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.username(), request.password());
    }

    @GetMapping("/planes")
    @SecurityRequirements
    public List<PlanResponse> registrationPlans() {
        return catalogs.plans();
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    public LoginResponse register(@Valid @RequestBody MemberRequest request) {
        return auth.register(request);
    }

    @GetMapping("/perfil")
    @SecurityRequirement(name = "bearerAuth")
    public ProfileResponse profile(@AuthenticationPrincipal AccountUserDetails user) {
        return new ProfileResponse(user.getUsername(), user.account().getRole(), user.memberId());
    }
}
