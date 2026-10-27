package io.github.zhijunio.sociallogin.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

public final class ConfiguredClientRegistrations implements ClientRegistrationRepository {

    private static final String REDIRECT_URI = "{baseUrl}/login/oauth2/code/{registrationId}";
    private static final String FACEBOOK_API_VERSION = "v26.0";

    private final Map<String, ClientRegistration> registrations;

    public ConfiguredClientRegistrations(SocialLoginProperties properties) {
        Map<String, ClientRegistration> configured = new LinkedHashMap<>();
        properties.providers().forEach((id, credentials) -> {
            if (credentials == null) {
                return;
            }
            boolean hasId = hasText(credentials.clientId());
            boolean hasSecret = hasText(credentials.clientSecret());
            if (hasId != hasSecret) {
                throw new IllegalStateException("Both client ID and secret must be configured for " + id);
            }
            if (hasId) {
                configured.put(id, registration(id, credentials));
            }
        });
        this.registrations = Collections.unmodifiableMap(configured);
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return registrations.get(registrationId);
    }

    public List<Provider> providers() {
        return registrations.keySet().stream()
                .map(id -> new Provider(id, displayName(id)))
                .toList();
    }

    private ClientRegistration registration(String id, SocialLoginProperties.Credentials credentials) {
        return switch (id) {
            case "github" ->
                CommonOAuth2Provider.GITHUB
                        .getBuilder(id)
                        .clientId(credentials.clientId())
                        .clientSecret(credentials.clientSecret())
                        .redirectUri(REDIRECT_URI)
                        .scope("read:user")
                        .build();
            case "google" ->
                CommonOAuth2Provider.GOOGLE
                        .getBuilder(id)
                        .clientId(credentials.clientId())
                        .clientSecret(credentials.clientSecret())
                        .redirectUri(REDIRECT_URI)
                        .build();
            case "facebook" -> facebookRegistration(id, credentials);
            case "apple" ->
                oidc(
                        id,
                        "Apple",
                        credentials,
                        "https://appleid.apple.com",
                        "https://appleid.apple.com/auth/authorize",
                        "https://appleid.apple.com/auth/token",
                        null,
                        "https://appleid.apple.com/auth/keys",
                        List.of("openid", "name", "email"));
            default -> throw new IllegalArgumentException("Unsupported social login provider: " + id);
        };
    }

    private ClientRegistration facebookRegistration(String id, SocialLoginProperties.Credentials credentials) {
        return CommonOAuth2Provider.FACEBOOK
                .getBuilder(id)
                .clientId(credentials.clientId())
                .clientSecret(credentials.clientSecret())
                .redirectUri(REDIRECT_URI)
                .authorizationUri("https://www.facebook.com/" + FACEBOOK_API_VERSION + "/dialog/oauth")
                .tokenUri("https://graph.facebook.com/" + FACEBOOK_API_VERSION + "/oauth/access_token")
                .userInfoUri("https://graph.facebook.com/" + FACEBOOK_API_VERSION + "/me?fields=id,name,email")
                .scope("public_profile", "email")
                .build();
    }

    private ClientRegistration oidc(
            String id,
            String name,
            SocialLoginProperties.Credentials credentials,
            String issuer,
            String authorizationUri,
            String tokenUri,
            String userInfoUri,
            String jwkSetUri,
            List<String> scopes) {
        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(id)
                .clientId(credentials.clientId())
                .clientSecret(credentials.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(REDIRECT_URI)
                .scope(scopes)
                .authorizationUri(authorizationUri)
                .tokenUri(tokenUri)
                .jwkSetUri(jwkSetUri)
                .issuerUri(issuer)
                .userNameAttributeName("sub")
                .clientName(name);
        if (userInfoUri != null) {
            builder.userInfoUri(userInfoUri);
        }
        return builder.build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String displayName(String id) {
        return switch (id) {
            case "github" -> "GitHub";
            case "google" -> "Google";
            case "facebook" -> "Facebook";
            case "apple" -> "Apple";
            default -> throw new IllegalArgumentException("Unsupported social login provider: " + id);
        };
    }

    public record Provider(String id, String name) {}
}
