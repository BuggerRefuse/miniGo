package org.example.domain.aggregate;

import org.example.domain.ports.PasswordMatcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UserWeChatAccountTest {

    @Test
    void weChatAccountIsIdentifiedByOpenIdAndCannotUsePasswordLogin() {
        User user = User.createWeChatAccount(
                "wx_demo",
                "openid-demo",
                "微信用户",
                "https://example.com/avatar.png"
        );

        PasswordMatcher matcher = (rawPassword, encodedPassword) -> false;

        assertEquals("openid-demo", user.getOpenId());
        assertFalse(user.canLoginWithPassword("123456", matcher));
    }
}
