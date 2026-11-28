package org.example.admin.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.admin.adapter.in.web.auth.CurrentAdminResolver;
import org.example.admin.application.dto.AdminLoginRequestDto;
import org.example.admin.application.dto.AdminResponseDto;
import org.example.admin.application.dto.RefreshTokenRequestDto;
import org.example.admin.application.dto.TokenResponseDto;
import org.example.admin.application.service.AdminAuthenticationApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthenticationApplicationService authenticationService;

    @PostMapping("/login")
    public TokenResponseDto login(@RequestBody AdminLoginRequestDto request) {
        return authenticationService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponseDto refresh(@RequestBody RefreshTokenRequestDto request) {
        return authenticationService.refresh(request);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(
            @RequestBody(required = false) RefreshTokenRequestDto request
    ) {
        authenticationService.logout(request);
        return Map.of("message", "管理员已退出登录");
    }

    @GetMapping("/me")
    public AdminResponseDto me(HttpServletRequest request) {
        return authenticationService.currentPrincipal(
                CurrentAdminResolver.resolve(request)
        );
    }
}
