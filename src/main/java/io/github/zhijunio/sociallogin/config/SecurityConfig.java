package io.github.zhijunio.sociallogin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    ConfiguredClientRegistrations clientRegistrations(SocialLoginProperties properties) {
        return new ConfiguredClientRegistrations(properties);
    }

    @Bean
    OAuth2AuthorizationRequestResolver authorizationRequestResolver(ClientRegistrationRepository registrations) {
        DefaultOAuth2AuthorizationRequestResolver delegate =
                new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(jakarta.servlet.http.HttpServletRequest request) {
                OAuth2AuthorizationRequest resolved = delegate.resolve(request);
                return formPostForApple(resolved, request.getRequestURI().endsWith("/apple"));
            }

            @Override
            public OAuth2AuthorizationRequest resolve(
                    jakarta.servlet.http.HttpServletRequest request, String clientRegistrationId) {
                return formPostForApple(
                        delegate.resolve(request, clientRegistrationId), "apple".equals(clientRegistrationId));
            }

            private OAuth2AuthorizationRequest formPostForApple(OAuth2AuthorizationRequest request, boolean apple) {
                if (request == null || !apple) {
                    return request;
                }
                return OAuth2AuthorizationRequest.from(request)
                        .additionalParameters(parameters -> parameters.put("response_mode", "form_post"))
                        .build();
            }
        };
    }

    @Bean
    SecurityFilterChain browserSecurity(
            HttpSecurity http, OAuth2AuthorizationRequestResolver authorizationRequestResolver) throws Exception {
        return http.csrf(csrf -> csrf.ignoringRequestMatchers(request -> "POST".equals(request.getMethod())
                        && "/login/oauth2/code/apple".equals(request.getServletPath())))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/assets/**", "/error", "/favicon.ico")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2Login(oauth2 -> oauth2.authorizationEndpoint(
                                endpoint -> endpoint.authorizationRequestResolver(authorizationRequestResolver))
                        .failureUrl("/?error"))
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .build();
    }
}
