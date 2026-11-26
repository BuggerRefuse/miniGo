package org.example.adapter.in.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.example.application.security.CurrentPrincipal;

public final class CurrentPrincipalResolver {

    private CurrentPrincipalResolver() {
    }

    public static CurrentPrincipal resolve(HttpServletRequest request) {
        Object principal = request.getAttribute(JwtAuthenticationFilter.PRINCIPAL_ATTRIBUTE);
        if (!(principal instanceof CurrentPrincipal currentPrincipal)) {
            throw new IllegalStateException("当前请求缺少登录身份");
        }
        return currentPrincipal;
    }
}
