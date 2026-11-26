const form = document.querySelector("#password-form");
const usernameInput = document.querySelector("#username");
const passwordInput = document.querySelector("#password");
const message = document.querySelector("#form-message");
const submitButton = document.querySelector("#submit-button");

function setBusy(busy) {
    submitButton.disabled = busy;
}

function enterWelcome(auth) {
    writeAuth({ tokens: auth, principal: auth.principal });
    window.location.replace("/welcome.html");
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    message.textContent = "";
    setBusy(true);
    try {
        const auth = await loginWithPassword(
            usernameInput.value.trim(),
            passwordInput.value
        );
        enterWelcome(auth);
    } catch (error) {
        message.textContent = error.message;
    } finally {
        setBusy(false);
    }
});
