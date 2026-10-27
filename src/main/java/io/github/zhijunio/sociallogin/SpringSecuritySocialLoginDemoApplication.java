package io.github.zhijunio.sociallogin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringSecuritySocialLoginDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringSecuritySocialLoginDemoApplication.class, args);
    }
}
