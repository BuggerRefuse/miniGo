package org.example.admin.application.service;

import org.example.admin.application.dto.AdminLoginRequestDto;
import org.example.admin.application.dto.RefreshTokenRequestDto;
import org.example.admin.application.dto.TokenResponseDto;
import org.example.admin.application.exception.AuthenticationFailedException;
import org.example.admin.application.security.JwtProperties;
import org.example.admin.application.security.JwtTokenService;
import org.example.admin.application.security.RefreshTokenSession;
import org.example.admin.application.security.RefreshTokenStore;
import org.example.admin.domain.aggregate.Admin;
import org.example.admin.domain.ports.AdminRepository;
import org.example.admin.domain.ports.PasswordMatcher;
import org.example.admin.domain.valueobject.AdminStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthenticationApplicationServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-27T02:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordMatcher passwordMatcher;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    private AdminAuthenticationApplicationService service;
    private Admin admin;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("admin-test-secret-with-at-least-32-bytes");
        properties.setIssuer("mini-go-client");
        service = new AdminAuthenticationApplicationService(
                adminRepository,
                passwordMatcher,
                new JwtTokenService(properties, CLOCK),
                refreshTokenStore,
                CLOCK
        );
        admin = Admin.restore(
                7L,
                "admin",
                "encoded-password",
                "系统管理员",
                AdminStatus.ACTIVE
        );
    }

    @Test
    void logsInAndStoresRefreshToken() {
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordMatcher.matches("123456", "encoded-password")).thenReturn(true);

        TokenResponseDto response = service.login(
                new AdminLoginRequestDto(" admin ", "123456")
        );

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.principal().id()).isEqualTo(7L);

        ArgumentCaptor<RefreshTokenSession> session =
                ArgumentCaptor.forClass(RefreshTokenSession.class);
        verify(refreshTokenStore).save(session.capture());
        assertThat(session.getValue().principalId()).isEqualTo(7L);
    }

    @Test
    void rejectsWrongPassword() {
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordMatcher.matches("wrong", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> service.login(
                new AdminLoginRequestDto("admin", "wrong")
        )).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void rotatesRefreshToken() {
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordMatcher.matches("123456", "encoded-password")).thenReturn(true);
        TokenResponseDto first = service.login(
                new AdminLoginRequestDto("admin", "123456")
        );

        when(refreshTokenStore.claim(anyString(), any(Instant.class))).thenReturn(true);
        when(adminRepository.findById(7L)).thenReturn(Optional.of(admin));
        TokenResponseDto refreshed = service.refresh(
                new RefreshTokenRequestDto(first.refreshToken())
        );

        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotEqualTo(first.refreshToken());
    }
}
