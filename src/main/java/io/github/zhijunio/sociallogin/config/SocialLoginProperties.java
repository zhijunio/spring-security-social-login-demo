package io.github.zhijunio.sociallogin.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("social-login")
public record SocialLoginProperties(Map<String, Credentials> providers) {

    public record Credentials(String clientId, String clientSecret) {}
}
