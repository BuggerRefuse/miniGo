package org.example.application.exception;

public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("用户名或密码错误");
    }

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
