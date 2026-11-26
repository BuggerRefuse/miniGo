package org.example.application.security;

import lombok.extern.slf4j.Slf4j;
import org.example.application.exception.AuthenticationFailedException;
import org.example.domain.ports.WeChatIdentityProvider;
import org.example.domain.valueobject.WeChatIdentity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;

@Component
@Slf4j
public class WeChatApiClient implements WeChatIdentityProvider {

    private final WeChatProperties properties;
    private final RestClient restClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public WeChatApiClient(WeChatProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public WeChatIdentity exchange(String loginCode) {
        if (loginCode == null || loginCode.isBlank()) {
            throw new IllegalArgumentException("微信登录 code 不能为空");
        }

        if (properties.isMockEnabled() && loginCode.startsWith("mock-")) {
            return new WeChatIdentity(mockOpenId(loginCode), null);
        }

        if (properties.getAppId().isBlank() || properties.getAppSecret().isBlank()) {
            throw new AuthenticationFailedException("微信登录尚未配置 AppID 和 AppSecret");
        }

        try {
            URI endpoint = URI.create(properties.getApiUrl());
            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme(endpoint.getScheme())
                            .host(endpoint.getHost())
                            .path(endpoint.getPath())
                            .queryParam("appid", properties.getAppId())
                            .queryParam("secret", properties.getAppSecret())
                            .queryParam("js_code", loginCode)
                            .queryParam("grant_type", "authorization_code")
                            .build())
                    .retrieve()
                    .body(String.class);

            WeChatSessionResponse response = parseResponse(responseBody);
            if (response == null || response.openid == null || response.openid.isBlank()) {
                String detail = response == null ? "空响应" : errorDetail(response);
                throw new AuthenticationFailedException("微信登录失败：" + detail);
            }
            return new WeChatIdentity(response.openid, response.unionid);
        } catch (RestClientException exception) {
            Throwable rootCause = exception;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            log.warn(
                    "WeChat token exchange failed at transport layer: {} -> {}",
                    exception.getClass().getSimpleName(),
                    rootCause.getClass().getSimpleName()
            );
            throw new AuthenticationFailedException("微信服务暂时不可用");
        } catch (JacksonException exception) {
            log.warn("WeChat returned a response that could not be parsed");
            throw new AuthenticationFailedException("微信服务返回了无法识别的数据");
        }
    }

    private WeChatSessionResponse parseResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        return jsonMapper.readValue(responseBody, WeChatSessionResponse.class);
    }

    private String errorDetail(WeChatSessionResponse response) {
        if (response.errcode == null) {
            return response.errmsg == null ? "未返回 openid" : response.errmsg;
        }
        if (response.errmsg == null || response.errmsg.isBlank()) {
            return String.valueOf(response.errcode);
        }
        return response.errcode + " " + response.errmsg;
    }

    private String mockOpenId(String loginCode) {
        String suffix = loginCode.substring("mock-".length())
                .replaceAll("[^A-Za-z0-9_-]", "-");
        if (suffix.isBlank()) {
            suffix = "demo";
        }
        return "openid-" + suffix;
    }

    private record WeChatSessionResponse(
            String openid,
            String unionid,
            Integer errcode,
            String errmsg
    ) {
    }
}
