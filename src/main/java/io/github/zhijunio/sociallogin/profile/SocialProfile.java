package io.github.zhijunio.sociallogin.profile;

import java.net.URI;

public record SocialProfile(String provider, String displayName, URI avatarUrl) {}
