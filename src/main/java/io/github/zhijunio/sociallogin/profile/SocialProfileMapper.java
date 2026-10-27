package io.github.zhijunio.sociallogin.profile;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SocialProfileMapper {

    private static final Pattern GITHUB_LOGIN = Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?");

    public SocialProfile map(String providerId, Map<String, Object> attributes, String principalName) {
        String displayName = text(attributes.get("name"));
        if (!hasText(displayName)) {
            displayName = text(attributes.get("preferred_username"));
        }
        if (!hasText(displayName) && "github".equals(providerId)) {
            String login = text(attributes.get("login"));
            displayName = hasText(login) && GITHUB_LOGIN.matcher(login).matches() ? login : null;
        }
        if (!hasText(displayName)) {
            displayName = switch (providerId) {
                case "github" -> principalName;
                case "google" -> "Google account";
                case "facebook" -> "Facebook account";
                case "apple" -> "Apple account";
                default -> "Social account";
            };
        }

        return new SocialProfile(providerName(providerId), displayName, safeAvatar(providerId, attributes));
    }

    private URI safeAvatar(String providerId, Map<String, Object> attributes) {
        String value = switch (providerId) {
            case "github" -> text(attributes.get("avatar_url"));
            case "google" -> text(attributes.get("picture"));
            case "facebook" -> facebookPicture(attributes.get("picture"));
            default -> null;
        };
        if (!hasText(value)) {
            return null;
        }
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if ("https".equalsIgnoreCase(uri.getScheme())
                    && uri.getPort() == -1
                    && uri.getUserInfo() == null
                    && trustedAvatarHost(providerId, host)) {
                return uri;
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }

    private boolean trustedAvatarHost(String providerId, String host) {
        if (host == null) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return switch (providerId) {
            case "github" -> "avatars.githubusercontent.com".equals(normalized);
            case "google" -> normalized.endsWith(".googleusercontent.com");
            case "facebook" -> normalized.endsWith(".fbcdn.net") || normalized.endsWith(".fbsbx.com");
            default -> false;
        };
    }

    private String facebookPicture(Object picture) {
        if (!(picture instanceof Map<?, ?> pictureMap) || !(pictureMap.get("data") instanceof Map<?, ?> dataMap)) {
            return null;
        }
        return text(dataMap.get("url"));
    }

    private String providerName(String providerId) {
        return switch (providerId) {
            case "github" -> "GitHub";
            case "google" -> "Google";
            case "facebook" -> "Facebook";
            case "apple" -> "Apple";
            default -> "Social provider";
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String text(Object value) {
        return value instanceof String string ? string : null;
    }
}
