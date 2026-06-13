package com.github.vagnerlg.auth.infrastructure.web;

import com.github.vagnerlg.auth.application.LoginService;
import com.github.vagnerlg.auth.application.LogoutService;
import com.github.vagnerlg.auth.application.RefreshTokenService;
import com.github.vagnerlg.auth.application.RegisterUserService;
import com.github.vagnerlg.auth.infrastructure.web.request.LoginRequest;
import com.github.vagnerlg.auth.infrastructure.web.request.LogoutRequest;
import com.github.vagnerlg.auth.infrastructure.web.request.RefreshRequest;
import com.github.vagnerlg.auth.infrastructure.web.request.RegisterRequest;
import com.github.vagnerlg.auth.infrastructure.web.response.ApiResponse;
import com.github.vagnerlg.auth.infrastructure.web.response.RegisterResponse;
import com.github.vagnerlg.auth.infrastructure.web.response.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
class AuthController {

    private final RegisterUserService registerUserService;
    private final LoginService loginService;
    private final LogoutService logoutService;
    private final RefreshTokenService refreshTokenService;

    AuthController(RegisterUserService registerUserService,
                   LoginService loginService,
                   LogoutService logoutService,
                   RefreshTokenService refreshTokenService) {
        this.registerUserService = registerUserService;
        this.loginService = loginService;
        this.logoutService = logoutService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        var user = registerUserService.register(request.username(), request.name(), request.password());
        return ApiResponse.of(new RegisterResponse(user.keycloakId(), user.username()));
    }

    @PostMapping("/login")
    ApiResponse<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        var token = loginService.login(request.username(), request.password());
        return ApiResponse.of(new TokenResponse(token.accessToken(), token.refreshToken(), token.expiresIn()));
    }

    @PostMapping("/refresh")
    ApiResponse<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        var token = refreshTokenService.refresh(request.refreshToken());
        return ApiResponse.of(new TokenResponse(token.accessToken(), token.refreshToken(), token.expiresIn()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@RequestBody @Valid LogoutRequest request) {
        logoutService.logout(request.accessToken(), request.refreshToken());
    }
}
