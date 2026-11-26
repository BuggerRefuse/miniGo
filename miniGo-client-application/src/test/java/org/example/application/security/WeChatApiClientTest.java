package org.example.application.security;

import org.example.application.exception.AuthenticationFailedException;
import org.example.domain.valueobject.WeChatIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeChatApiClientTest {

    @Test
    void exchangesLoginCodeForOpenIdWithoutReturningSessionKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WeChatProperties properties = properties();
        server.expect(requestTo("https://api.weixin.qq.com/sns/jscode2session"
                        + "?appid=wx-test-app"
                        + "&secret=test-secret"
                        + "&js_code=valid-code"
                        + "&grant_type=authorization_code"))
                .andRespond(withSuccess(
                        """
                                {
                                  "openid": "openid-123",
                                  "unionid": "union-456",
                                  "session_key": "must-not-leak"
                                }
                                """,
                        MediaType.TEXT_PLAIN
                ));

        WeChatIdentity identity = new WeChatApiClient(properties, builder).exchange("valid-code");

        assertEquals("openid-123", identity.openId());
        assertEquals("union-456", identity.unionId());
        server.verify();
    }

    @Test
    void reportsWeChatErrorCodeWhenCodeIsInvalid() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.weixin.qq.com/sns/jscode2session"
                        + "?appid=wx-test-app"
                        + "&secret=test-secret"
                        + "&js_code=expired-code"
                        + "&grant_type=authorization_code"))
                .andRespond(withSuccess(
                        """
                                {"errcode": 40029, "errmsg": "invalid code"}
                                """,
                        MediaType.APPLICATION_JSON
                ));

        AuthenticationFailedException exception = assertThrows(
                AuthenticationFailedException.class,
                () -> new WeChatApiClient(properties(), builder).exchange("expired-code")
        );

        assertTrue(exception.getMessage().contains("40029"));
        server.verify();
    }

    private WeChatProperties properties() {
        WeChatProperties properties = new WeChatProperties();
        properties.setAppId("wx-test-app");
        properties.setAppSecret("test-secret");
        properties.setMockEnabled(false);
        return properties;
    }
}
