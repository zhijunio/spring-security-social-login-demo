package io.github.zhijunio.sociallogin.web;

import io.github.zhijunio.sociallogin.config.ConfiguredClientRegistrations;
import io.github.zhijunio.sociallogin.profile.SocialProfileMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginPageController {

    private final SocialProfileMapper profileMapper;
    private final ConfiguredClientRegistrations registrations;

    public LoginPageController(SocialProfileMapper profileMapper, ConfiguredClientRegistrations registrations) {
        this.profileMapper = profileMapper;
        this.registrations = registrations;
    }

    @GetMapping("/")
    String home(
            @AuthenticationPrincipal OAuth2User principal,
            OAuth2AuthenticationToken authentication,
            @RequestParam(name = "error", required = false) String error,
            Model model) {
        LoginPageState state;
        if (principal != null) {
            String providerId = authentication.getAuthorizedClientRegistrationId();
            state = new LoginPageState.Authenticated(
                    profileMapper.map(providerId, principal.getAttributes(), principal.getName()));
        } else if (error != null) {
            state = new LoginPageState.LoginFailed();
        } else {
            state = new LoginPageState.Anonymous(registrations.providers());
        }
        model.addAttribute("page", state);
        return "index";
    }
}
