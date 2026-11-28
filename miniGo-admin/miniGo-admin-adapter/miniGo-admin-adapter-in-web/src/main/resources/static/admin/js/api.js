const ADMIN_AUTH_STORAGE_KEY = "miniGo.admin.auth";

function readAdminAuth() {
    const raw = sessionStorage.getItem(ADMIN_AUTH_STORAGE_KEY);
    if (!raw) {
        return null;
    }
    try {
        return JSON.parse(raw);
    } catch {
        sessionStorage.removeItem(ADMIN_AUTH_STORAGE_KEY);
        return null;
    }
}

function writeAdminAuth(tokens) {
    sessionStorage.setItem(ADMIN_AUTH_STORAGE_KEY, JSON.stringify({
        tokens,
        principal: tokens.principal
    }));
}

function clearAdminAuth() {
    sessionStorage.removeItem(ADMIN_AUTH_STORAGE_KEY);
}

async function readAdminError(response) {
    try {
        const payload = await response.json();
        return payload.message || "请求失败";
    } catch {
        return "请求失败";
    }
}

async function postAdminJson(path, body, accessToken) {
    const headers = { "Content-Type": "application/json" };
    if (accessToken) {
        headers.Authorization = `Bearer ${accessToken}`;
    }
    const response = await fetch(path, {
        method: "POST",
        headers,
        body: JSON.stringify(body)
    });
    if (!response.ok) {
        throw new Error(await readAdminError(response));
    }
    return response.status === 204 ? null : response.json();
}

async function loginAsAdmin(username, password) {
    const tokens = await postAdminJson("/api/admin/auth/login", {
        username,
        password
    });
    writeAdminAuth(tokens);
    return tokens;
}

async function refreshAdminTokens() {
    const auth = readAdminAuth();
    if (!auth?.tokens?.refreshToken) {
        throw new Error("管理员登录已失效");
    }
    const refreshed = await postAdminJson("/api/admin/auth/refresh", {
        refreshToken: auth.tokens.refreshToken
    });
    writeAdminAuth(refreshed);
    return refreshed;
}

async function adminFetch(path, options = {}, allowRefresh = true) {
    const auth = readAdminAuth();
    if (!auth?.tokens?.accessToken) {
        throw new Error("请先登录管理员账号");
    }

    const headers = new Headers(options.headers || {});
    headers.set("Authorization", `Bearer ${auth.tokens.accessToken}`);
    const response = await fetch(path, { ...options, headers });
    if (response.status !== 401 || !allowRefresh) {
        return response;
    }

    try {
        const refreshed = await refreshAdminTokens();
        headers.set("Authorization", `Bearer ${refreshed.accessToken}`);
        return fetch(path, { ...options, headers });
    } catch {
        clearAdminAuth();
        window.location.replace("/admin/login.html");
        throw new Error("管理员登录已失效");
    }
}

async function logoutAdmin() {
    const auth = readAdminAuth();
    if (auth?.tokens?.refreshToken) {
        try {
            await postAdminJson("/api/admin/auth/logout", {
                refreshToken: auth.tokens.refreshToken
            });
        } catch {
            // Clear the local session even if the refresh token has expired.
        }
    }
    clearAdminAuth();
    window.location.replace("/admin/login.html");
}
