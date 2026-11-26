package org.example.adapter.in.web.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.application.exception.InvalidTokenException;
import org.example.application.exception.ResourceNotFoundException;
import org.example.application.security.CurrentPrincipal;
import org.example.application.security.JwtTokenService;
import org.example.application.security.TokenClaims;
import org.example.application.service.AuthenticationApplicationService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String PRINCIPAL_ATTRIBUTE = "miniGo.currentPrincipal";

    private static final String API_PREFIX = "/api";
    private static final Set<String> PUBLIC_API_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/wechat",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/users/register"
    );

    private final JwtTokenService jwtTokenService;
    private final AuthenticationApplicationService authenticationService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = requestPath(request);
        if (!path.equals(API_PREFIX) && !path.startsWith(API_PREFIX + "/")) {
            return true;
        }
        return PUBLIC_API_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
            return;
        }

        try {
            TokenClaims claims = jwtTokenService.parseAccessToken(
                    authorization.substring("Bearer ".length()).trim()
            );
            CurrentPrincipal principal = authenticationService.verifyCurrentPrincipal(
                    new CurrentPrincipal(
                            claims.principalId(),
                            claims.username(),
                            claims.displayName(),
                            claims.avatarUrl()
                    )
            );
            request.setAttribute(PRINCIPAL_ATTRIBUTE, principal);
            filterChain.doFilter(request, response);
        } catch (InvalidTokenException | ResourceNotFoundException exception) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
        }
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String safeMessage = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"message\":\"" + safeMessage + "\"}");
    }

    private String requestPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.length() > 1 && path.endsWith("/")
                ? path.substring(0, path.length() - 1)
                : path;
    }
}
