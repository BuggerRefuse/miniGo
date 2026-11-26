const greeting = document.querySelector("#greeting");
const subtitle = document.querySelector("#welcome-subtitle");
const username = document.querySelector("#profile-username");
const displayName = document.querySelector("#profile-name");
const accountId = document.querySelector("#profile-id");
const sessionState = document.querySelector("#profile-session");
const logoutButton = document.querySelector("#logout-button");
const refreshButton = document.querySelector("#refresh-button");

function renderPrincipal(principal) {
    greeting.textContent = `欢迎，${principal.displayName}`;
    subtitle.textContent = "登录成功，可以继续使用 miniGo。";
    username.textContent = principal.username;
    displayName.textContent = principal.displayName;
    accountId.textContent = String(principal.id);
    sessionState.textContent = "有效";
}

async function loadWelcome() {
    try {
        const response = await authFetch("/api/auth/me");
        if (!response.ok) {
            throw new Error(await readError(response));
        }
        const principal = await response.json();
        renderPrincipal(principal);
    } catch (error) {
        if (error.message === "请先登录" || error.message === "登录已失效") {
            return;
        }
        greeting.textContent = "暂时无法加载";
        subtitle.textContent = error.message;
    }
}

refreshButton.addEventListener("click", async () => {
    refreshButton.disabled = true;
    try {
        await refreshTokens();
        await loadWelcome();
    } finally {
        refreshButton.disabled = false;
    }
});

logoutButton.addEventListener("click", logout);

if (!readAuth()) {
    window.location.replace("/login.html");
} else {
    loadWelcome();
}
