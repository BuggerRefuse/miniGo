package org.example.admin.adapter.in.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.example.admin.application.security.CurrentPrincipal;

public final class CurrentAdminResolver {

    private CurrentAdminResolver() {
    }

    public static CurrentPrincipal resolve(HttpServletRequest request) {
        Object principal = request.getAttribute(
                JwtAdminAuthenticationFilter.ADMIN_PRINCIPAL_ATTRIBUTE
        );
        if (!(principal instanceof CurrentPrincipal currentPrincipal)) {
            throw new IllegalStateException("当前请求缺少管理员登录身份");
        }
        return currentPrincipal;
    }
}
