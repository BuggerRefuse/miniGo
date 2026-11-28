const displayName = document.querySelector("#admin-display-name");
const username = document.querySelector("#admin-username");
const adminId = document.querySelector("#admin-id");
const tokenExpiry = document.querySelector("#token-expiry");
const profileStatus = document.querySelector("#profile-status");
const refreshButton = document.querySelector("#refresh-button");
const logoutButton = document.querySelector("#logout-button");
const message = document.querySelector("#dashboard-message");

async function loadProfile() {
    message.textContent = "";
    profileStatus.textContent = "正在加载";
    try {
        const response = await adminFetch("/api/admin/auth/me");
        if (!response.ok) {
            throw new Error(await readAdminError(response));
        }
        const principal = await response.json();
        const auth = readAdminAuth();
        displayName.textContent = principal.displayName || "管理员";
        username.textContent = principal.username;
        adminId.textContent = principal.id;
        tokenExpiry.textContent = `约 ${Math.max(
            1,
            Math.round((auth?.tokens?.expiresIn || 0) / 60)
        )} 分钟`;
        profileStatus.textContent = "身份已确认";
    } catch (error) {
        profileStatus.textContent = "加载失败";
        message.textContent = error instanceof Error
            ? error.message
            : "管理员信息加载失败";
    }
}

refreshButton.addEventListener("click", async () => {
    refreshButton.disabled = true;
    message.textContent = "";
    try {
        await refreshAdminTokens();
        await loadProfile();
        message.textContent = "登录状态已刷新";
    } catch (error) {
        message.textContent = error instanceof Error
            ? error.message
            : "登录状态刷新失败";
    } finally {
        refreshButton.disabled = false;
    }
});

logoutButton.addEventListener("click", async () => {
    logoutButton.disabled = true;
    await logoutAdmin();
});

loadProfile();
