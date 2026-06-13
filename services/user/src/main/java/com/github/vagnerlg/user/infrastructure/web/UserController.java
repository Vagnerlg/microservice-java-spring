package com.github.vagnerlg.user.infrastructure.web;

import com.github.vagnerlg.user.application.UserService;
import com.github.vagnerlg.user.infrastructure.web.response.ApiResponse;
import com.github.vagnerlg.user.infrastructure.web.response.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    ApiResponse<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.of(UserResponse.from(userService.findByKeycloakId(jwt.getSubject())));
    }
}
