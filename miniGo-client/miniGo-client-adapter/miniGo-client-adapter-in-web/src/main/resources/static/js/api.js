const AUTH_STORAGE_KEY = "miniGo.auth";

function readAuth() {
    const raw = sessionStorage.getItem(AUTH_STORAGE_KEY);
    if (!raw) {
        return null;
    }
    try {
        return JSON.parse(raw);
    } catch {
        sessionStorage.removeItem(AUTH_STORAGE_KEY);
        return null;
    }
}

function writeAuth(auth) {
    sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
}

function clearAuth() {
    sessionStorage.removeItem(AUTH_STORAGE_KEY);
}

async function readError(response) {
    try {
        const payload = await response.json();
        return payload.message || "请求失败";
    } catch {
        return "请求失败";
    }
}

async function postJson(path, body, accessToken) {
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
        throw new Error(await readError(response));
    }
    return response.status === 204 ? null : response.json();
}

async function loginWithPassword(username, password) {
    return postJson("/api/auth/login", { username, password });
}

async function refreshTokens() {
    const auth = readAuth();
    if (!auth?.tokens?.refreshToken) {
        throw new Error("登录已失效");
    }
    const refreshed = await postJson("/api/auth/refresh", {
        refreshToken: auth.tokens.refreshToken
    });
    writeAuth({ tokens: refreshed, principal: refreshed.principal });
    return refreshed;
}

async function authFetch(path, options = {}, allowRefresh = true) {
    const auth = readAuth();
    if (!auth?.tokens?.accessToken) {
        throw new Error("请先登录");
    }

    const headers = new Headers(options.headers || {});
    headers.set("Authorization", `Bearer ${auth.tokens.accessToken}`);
    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    const response = await fetch(path, { ...options, headers });
    if (response.status === 401 && allowRefresh) {
        try {
            const refreshed = await refreshTokens();
            headers.set("Authorization", `Bearer ${refreshed.accessToken}`);
            return fetch(path, { ...options, headers });
        } catch {
            clearAuth();
            window.location.replace("/login.html");
            throw new Error("登录已失效");
        }
    }
    return response;
}

async function logout() {
    const auth = readAuth();
    if (auth?.tokens?.refreshToken) {
        try {
            await postJson("/api/auth/logout", {
                refreshToken: auth.tokens.refreshToken
            });
        } catch {
            // Local cleanup follows even if the server token is already expired.
        }
    }
    clearAuth();
    window.location.replace("/login.html");
}
