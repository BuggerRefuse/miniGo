package org.example.admin.application.service;

import lombok.RequiredArgsConstructor;
import org.example.admin.application.dto.AdminLoginRequestDto;
import org.example.admin.application.dto.AdminResponseDto;
import org.example.admin.application.dto.RefreshTokenRequestDto;
import org.example.admin.application.dto.TokenResponseDto;
import org.example.admin.application.exception.AuthenticationFailedException;
import org.example.admin.application.exception.InvalidTokenException;
import org.example.admin.application.exception.ResourceNotFoundException;
import org.example.admin.application.security.CurrentPrincipal;
import org.example.admin.application.security.IssuedTokens;
import org.example.admin.application.security.JwtTokenService;
import org.example.admin.application.security.RefreshTokenSession;
import org.example.admin.application.security.RefreshTokenStore;
import org.example.admin.application.security.TokenClaims;
import org.example.admin.domain.aggregate.Admin;
import org.example.admin.domain.ports.AdminRepository;
import org.example.admin.domain.ports.PasswordMatcher;
import org.example.admin.domain.valueobject.AdminStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class AdminAuthenticationApplicationService {

    private final AdminRepository adminRepository;
    private final PasswordMatcher passwordMatcher;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenStore refreshTokenStore;
    private final Clock clock;

    @Transactional
    public TokenResponseDto login(AdminLoginRequestDto request) {
        validateLogin(request);
        Admin admin = adminRepository.findByUsername(request.username().trim())
                .orElseThrow(AuthenticationFailedException::new);
        if (!admin.canLogin(request.password(), passwordMatcher)) {
            throw new AuthenticationFailedException();
        }
        return issueTokens(toPrincipal(admin));
    }

    @Transactional
    public TokenResponseDto refresh(RefreshTokenRequestDto request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new IllegalArgumentException("refreshToken 不能为空");
        }

        TokenClaims claims = jwtTokenService.parseRefreshToken(request.refreshToken());
        if (!refreshTokenStore.claim(claims.tokenId(), clock.instant())) {
            throw new InvalidTokenException("刷新令牌已使用或已失效");
        }

        return issueTokens(requireActivePrincipal(claims));
    }

    @Transactional
    public void logout(RefreshTokenRequestDto request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            return;
        }
        TokenClaims claims = jwtTokenService.parseRefreshToken(request.refreshToken());
        refreshTokenStore.revoke(claims.tokenId(), clock.instant());
    }

    public AdminResponseDto currentPrincipal(CurrentPrincipal principal) {
        return toResponse(verifyCurrentPrincipal(principal));
    }

    public CurrentPrincipal verifyCurrentPrincipal(CurrentPrincipal principal) {
        return adminRepository.findById(principal.id())
                .filter(admin -> admin.getStatus() == AdminStatus.ACTIVE)
                .map(this::toPrincipal)
                .orElseThrow(() -> new ResourceNotFoundException("管理员不存在或已停用"));
    }

    private TokenResponseDto issueTokens(CurrentPrincipal principal) {
        IssuedTokens tokens = jwtTokenService.issueTokens(principal);
        refreshTokenStore.save(new RefreshTokenSession(
                tokens.refreshTokenId(),
                principal.id(),
                tokens.refreshTokenExpiresAt()
        ));
        return new TokenResponseDto(
                "Bearer",
                tokens.accessToken(),
                tokens.accessTokenExpiresInSeconds(),
                tokens.refreshToken(),
                toResponse(principal)
        );
    }

    private CurrentPrincipal requireActivePrincipal(TokenClaims claims) {
        return adminRepository.findById(claims.principalId())
                .filter(admin -> admin.getStatus() == AdminStatus.ACTIVE)
                .map(this::toPrincipal)
                .orElseThrow(() -> new InvalidTokenException("管理员不存在或已停用"));
    }

    private CurrentPrincipal toPrincipal(Admin admin) {
        return new CurrentPrincipal(
                admin.getId(),
                admin.getUsername(),
                admin.getDisplayName()
        );
    }

    private AdminResponseDto toResponse(CurrentPrincipal principal) {
        return new AdminResponseDto(
                principal.id(),
                principal.username(),
                principal.displayName()
        );
    }

    private void validateLogin(AdminLoginRequestDto request) {
        if (request == null
                || request.username() == null
                || request.username().isBlank()
                || request.password() == null
                || request.password().isBlank()) {
            throw new IllegalArgumentException("管理员用户名和密码不能为空");
        }
    }
}
