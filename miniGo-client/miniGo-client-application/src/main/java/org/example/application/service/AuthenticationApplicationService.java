package org.example.application.service;

import lombok.RequiredArgsConstructor;
import org.example.application.dto.CurrentPrincipalResponseDto;
import org.example.application.dto.PasswordLoginRequestDto;
import org.example.application.dto.RefreshTokenRequestDto;
import org.example.application.dto.TokenResponseDto;
import org.example.application.dto.WeChatLoginRequestDto;
import org.example.application.exception.AuthenticationFailedException;
import org.example.application.exception.InvalidTokenException;
import org.example.application.exception.ResourceNotFoundException;
import org.example.application.security.CurrentPrincipal;
import org.example.application.security.IssuedTokens;
import org.example.application.security.JwtTokenService;
import org.example.application.security.RefreshTokenSession;
import org.example.application.security.RefreshTokenStore;
import org.example.application.security.TokenClaims;
import org.example.domain.aggregate.User;
import org.example.domain.ports.PasswordMatcher;
import org.example.domain.ports.UserRepository;
import org.example.domain.ports.WeChatIdentityProvider;
import org.example.domain.service.UserDomainService;
import org.example.domain.valueobject.AccountStatus;
import org.example.domain.valueobject.WeChatIdentity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthenticationApplicationService {

    private final UserRepository userRepository;
    private final PasswordMatcher passwordMatcher;
    private final WeChatIdentityProvider weChatIdentityProvider;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenStore refreshTokenStore;
    private final Clock clock;
    private final UserDomainService userDomainService = new UserDomainService();

    @Transactional
    public TokenResponseDto login(PasswordLoginRequestDto request) {
        validatePasswordLogin(request);
        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(AuthenticationFailedException::new);
        if (!userDomainService.login(user, request.password(), passwordMatcher)) {
            throw new AuthenticationFailedException();
        }
        return issueTokens(toPrincipal(user));
    }

    @Transactional
    public TokenResponseDto loginWithWeChat(WeChatLoginRequestDto request) {
        if (request == null || request.code() == null || request.code().isBlank()) {
            throw new IllegalArgumentException("微信登录 code 不能为空");
        }

        WeChatIdentity identity = weChatIdentityProvider.exchange(request.code().trim());
        User user = userRepository.findByOpenId(identity.openId())
                .orElseGet(() -> registerWeChatUser(identity));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AuthenticationFailedException("当前账号已被停用");
        }
        return issueTokens(toPrincipal(user));
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

        CurrentPrincipal principal = requireActivePrincipal(claims);
        return issueTokens(principal);
    }

    @Transactional
    public void logout(RefreshTokenRequestDto request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            return;
        }
        TokenClaims claims = jwtTokenService.parseRefreshToken(request.refreshToken());
        refreshTokenStore.revoke(claims.tokenId(), clock.instant());
    }

    public CurrentPrincipalResponseDto currentPrincipal(CurrentPrincipal principal) {
        return toResponse(verifyCurrentPrincipal(principal));
    }

    public CurrentPrincipal verifyCurrentPrincipal(CurrentPrincipal principal) {
        return userRepository.findById(principal.id())
                .filter(user -> user.getStatus() == AccountStatus.ACTIVE)
                .map(this::toPrincipal)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在或已停用"));
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
        return userRepository.findById(claims.principalId())
                .filter(user -> user.getStatus() == AccountStatus.ACTIVE)
                .map(this::toPrincipal)
                .orElseThrow(() -> new InvalidTokenException("用户不存在或已停用"));
    }

    private User registerWeChatUser(WeChatIdentity identity) {
        String suffix = identity.openId().length() > 12
                ? identity.openId().substring(identity.openId().length() - 12)
                : identity.openId();
        String username = "wx_" + shortHash(identity.openId());
        String nickname = "微信用户" + suffix.substring(Math.max(0, suffix.length() - 4));
        User user = User.createWeChatAccount(username, identity.openId(), nickname, null);
        return userRepository.save(user);
    }

    private CurrentPrincipal toPrincipal(User user) {
        return new CurrentPrincipal(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl()
        );
    }

    private CurrentPrincipalResponseDto toResponse(CurrentPrincipal principal) {
        return new CurrentPrincipalResponseDto(
                principal.id(),
                principal.username(),
                principal.displayName(),
                principal.avatarUrl()
        );
    }

    private void validatePasswordLogin(PasswordLoginRequestDto request) {
        if (request == null
                || request.username() == null
                || request.username().isBlank()
                || request.password() == null
                || request.password().isBlank()) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
    }

    private String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
