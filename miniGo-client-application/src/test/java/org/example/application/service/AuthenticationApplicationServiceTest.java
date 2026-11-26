package org.example.application.service;

import org.example.application.dto.PasswordLoginRequestDto;
import org.example.application.dto.RefreshTokenRequestDto;
import org.example.application.dto.TokenResponseDto;
import org.example.application.dto.WeChatLoginRequestDto;
import org.example.application.exception.InvalidTokenException;
import org.example.application.security.BcryptPasswordCodec;
import org.example.application.security.JwtProperties;
import org.example.application.security.JwtTokenService;
import org.example.application.security.RefreshTokenSession;
import org.example.application.security.RefreshTokenStore;
import org.example.domain.aggregate.User;
import org.example.domain.ports.UserRepository;
import org.example.domain.valueobject.AccountStatus;
import org.example.domain.valueobject.WeChatIdentity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationApplicationServiceTest {

    private final BcryptPasswordCodec codec = new BcryptPasswordCodec();
    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final InMemoryRefreshTokenStore refreshTokenStore = new InMemoryRefreshTokenStore();
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-11-25T08:00:00Z"),
            ZoneOffset.UTC
    );
    private final JwtTokenService jwtTokenService = jwtTokenService();

    @Test
    void passwordUserReceivesAccessAndRefreshTokens() {
        userRepository.save(User.createPasswordAccount(
                "alice",
                "123456",
                "Alice",
                null,
                null,
                codec
        ));
        AuthenticationApplicationService service = authenticationService();

        TokenResponseDto response = service.login(
                new PasswordLoginRequestDto("alice", "123456")
        );

        assertEquals("Bearer", response.tokenType());
        assertFalse(response.accessToken().isBlank());
        assertFalse(response.refreshToken().isBlank());
        assertFalse(jwtTokenService.parseAccessToken(response.accessToken()).tokenId().isBlank());
    }

    @Test
    void firstWeChatLoginCreatesAUserAndLaterLoginsReuseIt() {
        AuthenticationApplicationService service = authenticationService();

        TokenResponseDto first = service.loginWithWeChat(
                new WeChatLoginRequestDto("mock-demo-user")
        );
        TokenResponseDto second = service.loginWithWeChat(
                new WeChatLoginRequestDto("mock-demo-user")
        );

        assertEquals(first.principal().id(), second.principal().id());
        assertTrue(first.principal().username().startsWith("wx_"));
        assertEquals(1, userRepository.users.size());
    }

    @Test
    void refreshTokenCanOnlyBeUsedOnce() {
        userRepository.save(User.createPasswordAccount(
                "alice",
                "123456",
                "Alice",
                null,
                null,
                codec
        ));
        AuthenticationApplicationService service = authenticationService();
        TokenResponseDto login = service.login(
                new PasswordLoginRequestDto("alice", "123456")
        );

        TokenResponseDto refreshed = service.refresh(
                new RefreshTokenRequestDto(login.refreshToken())
        );

        assertFalse(refreshed.accessToken().equals(login.accessToken()));
        assertThrows(
                InvalidTokenException.class,
                () -> service.refresh(new RefreshTokenRequestDto(login.refreshToken()))
        );
    }

    @Test
    void disabledUserCannotRefreshTokens() {
        User user = userRepository.save(User.createPasswordAccount(
                "alice",
                "123456",
                "Alice",
                null,
                null,
                codec
        ));
        AuthenticationApplicationService service = authenticationService();
        TokenResponseDto login = service.login(
                new PasswordLoginRequestDto("alice", "123456")
        );
        userRepository.save(user.changeStatus(AccountStatus.DISABLED));

        assertThrows(
                InvalidTokenException.class,
                () -> service.refresh(new RefreshTokenRequestDto(login.refreshToken()))
        );
    }

    private AuthenticationApplicationService authenticationService() {
        return new AuthenticationApplicationService(
                userRepository,
                codec,
                code -> new WeChatIdentity("openid-" + code.substring("mock-".length()), null),
                jwtTokenService,
                refreshTokenStore,
                clock
        );
    }

    private JwtTokenService jwtTokenService() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-that-is-at-least-32-bytes-long");
        return new JwtTokenService(properties, clock);
    }

    private static final class InMemoryUserRepository implements UserRepository {

        private final AtomicLong ids = new AtomicLong();
        private final Map<Long, User> users = new LinkedHashMap<>();

        @Override
        public Optional<User> findById(Long id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return users.values().stream()
                    .filter(user -> user.getUsername().equals(username))
                    .findFirst();
        }

        @Override
        public Optional<User> findByOpenId(String openId) {
            return users.values().stream()
                    .filter(user -> openId.equals(user.getOpenId()))
                    .findFirst();
        }

        @Override
        public User save(User user) {
            if (user.getId() == null) {
                user.assignId(ids.incrementAndGet());
            }
            users.put(user.getId(), user);
            return user;
        }

        @Override
        public void deleteById(Long id) {
            users.remove(id);
        }
    }

    private static final class InMemoryRefreshTokenStore implements RefreshTokenStore {

        private final Map<String, RefreshTokenSession> active = new LinkedHashMap<>();

        @Override
        public void save(RefreshTokenSession session) {
            active.put(session.tokenId(), session);
        }

        @Override
        public boolean claim(String tokenId, Instant claimedAt) {
            return active.remove(tokenId) != null;
        }

        @Override
        public void revoke(String tokenId, Instant revokedAt) {
            active.remove(tokenId);
        }
    }
}
