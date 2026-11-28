const form = document.querySelector("#admin-login-form");
const usernameInput = document.querySelector("#admin-username");
const passwordInput = document.querySelector("#admin-password");
const submitButton = document.querySelector("#admin-submit");
const message = document.querySelector("#admin-message");

function setBusy(busy) {
    submitButton.disabled = busy;
    submitButton.textContent = busy ? "正在验证..." : "登录管理后台";
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    message.textContent = "";

    const username = usernameInput.value.trim();
    const password = passwordInput.value;
    if (!username || !password) {
        message.textContent = "请输入管理员账号和密码";
        return;
    }

    setBusy(true);
    try {
        await loginAsAdmin(username, password);
        window.location.replace("/admin/welcome.html");
    } catch (error) {
        message.textContent = error instanceof Error
            ? error.message
            : "管理员登录失败，请稍后重试";
    } finally {
        setBusy(false);
    }
});
