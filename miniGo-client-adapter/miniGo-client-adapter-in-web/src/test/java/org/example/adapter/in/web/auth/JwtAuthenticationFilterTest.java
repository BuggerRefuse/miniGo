package org.example.adapter.in.web.auth;

import org.example.application.security.JwtTokenService;
import org.example.application.service.AuthenticationApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(
                mock(JwtTokenService.class),
                mock(AuthenticationApplicationService.class)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/auth/login",
            "/api/auth/wechat",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/users/register"
    })
    void publicApiPathsBypassAuthentication(String path) {
        assertTrue(filter.shouldNotFilter(request("POST", path)));
    }

    @Test
    void nonApiResourcesBypassAuthentication() {
        assertTrue(filter.shouldNotFilter(request("GET", "/login.html")));
    }

    @Test
    void optionsRequestsBypassAuthentication() {
        assertTrue(filter.shouldNotFilter(request("OPTIONS", "/api/games/leaderboard")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api",
            "/api/",
            "/api/games/leaderboard",
            "/api/users/me",
            "/api/auth/me",
            "/api/auth/login/other"
    })
    void unlistedApiPathsRequireAuthentication(String path) {
        assertFalse(filter.shouldNotFilter(request("GET", path)));
    }

    @Test
    void contextPathIsExcludedFromPublicPathMatching() {
        MockHttpServletRequest request = request("POST", "/mini-go/api/auth/login");
        request.setContextPath("/mini-go");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void unlistedApiWithoutBearerTokenReturnsUnauthorized() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/games/leaderboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    private MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }
}
