package io.github.zhijunio.sociallogin;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "social-login.providers.github.client-id=test-github-id",
            "social-login.providers.github.client-secret=test-github-secret",
            "social-login.providers.google.client-id=test-google-id",
            "social-login.providers.google.client-secret=test-google-secret",
            "social-login.providers.facebook.client-id=test-facebook-id",
            "social-login.providers.facebook.client-secret=test-facebook-secret",
            "social-login.providers.apple.client-id=test-apple-id",
            "social-login.providers.apple.client-secret=test-apple-secret"
        })
@AutoConfigureMockMvc
class SocialLoginPageTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void anonymousVisitorSeesOnlyConfiguredProvidersAndTheThreeStepFlow() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/oauth2/authorization/github")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/oauth2/authorization/google")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/oauth2/authorization/facebook")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/oauth2/authorization/apple")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Provider authorization")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Spring Security callback")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Application session")));
    }

    @Test
    void appleAuthorizationUsesFormPostAndOmitsCsrfOnlyOnItsCallback() throws Exception {
        mvc.perform(get("/oauth2/authorization/apple").with(request -> {
                    request.setServerName("localhost");
                    request.setServerPort(8080);
                    return request;
                }))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://appleid.apple.com/auth/authorize?")))
                .andExpect(
                        header().string("Location", org.hamcrest.Matchers.containsString("response_mode=form_post")));
        mvc.perform(post("/login/oauth2/code/apple")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/login/oauth2/code/github")).andExpect(status().isForbidden());
    }

    @Test
    void githubLoginStartsAProviderRedirect() throws Exception {
        mvc.perform(get("/oauth2/authorization/github").with(request -> {
                    request.setServerName("localhost");
                    request.setServerPort(8080);
                    return request;
                }))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://github.com/login/oauth/authorize?")))
                .andExpect(header().string(
                                "Location",
                                org.hamcrest.Matchers.containsString(
                                        "redirect_uri=http://localhost:8080/login/oauth2/code/github")));
    }

    @Test
    void loginFailureShowsOnlyGenericRecoveryCopy() throws Exception {
        mvc.perform(get("/").queryParam("error", "provider-detail-must-not-appear"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sign-in could not be completed")))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("provider-detail-must-not-appear"))))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("test-secret"))));
    }

    @Test
    void authenticatedVisitorSeesMappedProfileAndCsrfProtectedLogout() throws Exception {
        mvc.perform(get("/").with(githubLogin().attributes(attributes -> {
                    attributes.put("login", "octo-cat");
                    attributes.put("name", "Octo Cat");
                    attributes.put("avatar_url", "https://avatars.githubusercontent.com/u/123?v=4");
                    attributes.put("html_url", "https://github.com/octo-cat");
                    attributes.put("access_token", "token-value-must-not-appear");
                })))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Octo Cat")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Signed in with GitHub")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/logout\"")))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("token-value-must-not-appear"))));
    }

    @Test
    void profileUsesSafeFallbacksForOptionalProviderFields() throws Exception {
        mvc.perform(get("/").with(githubLogin().attributes(attributes -> {
                    attributes.put("login", "octo-cat");
                    attributes.put("avatar_url", "javascript:alert(1)");
                    attributes.put("html_url", "https://attacker.example/profile");
                })))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("octo-cat")))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("javascript:alert(1)"))))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("attacker.example"))));
    }

    @Test
    void logoutRejectsMissingCsrfToken() throws Exception {
        mvc.perform(post("/logout").with(githubLogin())).andExpect(status().isForbidden());
    }

    @Test
    void logoutAcceptsCsrfProtectedPost() throws Exception {
        mvc.perform(post("/logout").with(githubLogin()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/"));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                    .OAuth2LoginRequestPostProcessor
            githubLogin() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("github")
                .clientId("test-github-id")
                .clientSecret("test-github-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("login")
                .clientName("GitHub")
                .build();
        return oauth2Login().clientRegistration(registration);
    }
}
