package org.example.admin.adapter.in.web.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.admin.application.exception.InvalidTokenException;
import org.example.admin.application.exception.ResourceNotFoundException;
import org.example.admin.application.security.CurrentPrincipal;
import org.example.admin.application.security.JwtTokenService;
import org.example.admin.application.security.TokenClaims;
import org.example.admin.application.service.AdminAuthenticationApplicationService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAdminAuthenticationFilter extends OncePerRequestFilter {

    public static final String ADMIN_PRINCIPAL_ATTRIBUTE = "miniGo.currentAdmin";

    private static final String ADMIN_API_PREFIX = "/api/admin";
    private static final Set<String> PUBLIC_API_PATHS = Set.of(
            "/api/admin/auth/login",
            "/api/admin/auth/refresh",
            "/api/admin/auth/logout"
    );

    private final JwtTokenService jwtTokenService;
    private final AdminAuthenticationApplicationService authenticationService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = requestPath(request);
        if (!path.equals(ADMIN_API_PREFIX) && !path.startsWith(ADMIN_API_PREFIX + "/")) {
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
        if (authorization == null
                || !authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录管理员账号");
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
                            claims.displayName()
                    )
            );
            request.setAttribute(ADMIN_PRINCIPAL_ATTRIBUTE, principal);
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
