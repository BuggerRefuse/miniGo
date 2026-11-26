package org.example.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.adapter.in.web.auth.CurrentPrincipalResolver;
import org.example.application.dto.CurrentPrincipalResponseDto;
import org.example.application.dto.PasswordLoginRequestDto;
import org.example.application.dto.RefreshTokenRequestDto;
import org.example.application.dto.TokenResponseDto;
import org.example.application.dto.WeChatLoginRequestDto;
import org.example.application.service.AuthenticationApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationApplicationService authenticationService;

    @PostMapping("/login")
    public TokenResponseDto login(@RequestBody PasswordLoginRequestDto request) {
        return authenticationService.login(request);
    }

    @PostMapping("/wechat")
    public TokenResponseDto loginWithWeChat(@RequestBody WeChatLoginRequestDto request) {
        return authenticationService.loginWithWeChat(request);
    }

    @PostMapping("/refresh")
    public TokenResponseDto refresh(@RequestBody RefreshTokenRequestDto request) {
        return authenticationService.refresh(request);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestBody(required = false) RefreshTokenRequestDto request) {
        authenticationService.logout(request);
        return Map.of("message", "已退出登录");
    }

    @GetMapping("/me")
    public CurrentPrincipalResponseDto me(HttpServletRequest request) {
        return authenticationService.currentPrincipal(
                CurrentPrincipalResolver.resolve(request)
        );
    }
}
