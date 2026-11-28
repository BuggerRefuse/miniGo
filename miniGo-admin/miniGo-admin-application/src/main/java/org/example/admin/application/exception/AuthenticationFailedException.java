package org.example.admin.application.exception;

public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("管理员账号或密码错误");
    }

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
