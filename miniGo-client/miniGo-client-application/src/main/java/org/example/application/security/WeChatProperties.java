package org.example.application.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mini-go.auth.wechat")
public class WeChatProperties {

    private String appId = "";
    private String appSecret = "";
    private String apiUrl = "https://api.weixin.qq.com/sns/jscode2session";
    private boolean mockEnabled = true;
}
