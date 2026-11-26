package org.example.domain.valueobject;

public record WeChatIdentity(String openId, String unionId) {

    public WeChatIdentity {
        if (openId == null || openId.isBlank()) {
            throw new IllegalArgumentException("微信 openId 不能为空");
        }
    }
}
