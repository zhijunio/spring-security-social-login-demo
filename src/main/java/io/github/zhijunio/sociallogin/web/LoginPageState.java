package io.github.zhijunio.sociallogin.web;

import io.github.zhijunio.sociallogin.config.ConfiguredClientRegistrations.Provider;
import io.github.zhijunio.sociallogin.profile.SocialProfile;
import java.util.List;

public sealed interface LoginPageState
        permits LoginPageState.Anonymous, LoginPageState.LoginFailed, LoginPageState.Authenticated {

    default boolean isAuthenticated() {
        return false;
    }

    default boolean isLoginFailed() {
        return false;
    }

    default SocialProfile getProfile() {
        return null;
    }

    default List<Provider> getProviders() {
        return List.of();
    }

    record Anonymous(List<Provider> providers) implements LoginPageState {
        @Override
        public List<Provider> getProviders() {
            return providers;
        }
    }

    record LoginFailed() implements LoginPageState {
        @Override
        public boolean isLoginFailed() {
            return true;
        }
    }

    record Authenticated(SocialProfile profile) implements LoginPageState {
        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public SocialProfile getProfile() {
            return profile;
        }
    }
}
