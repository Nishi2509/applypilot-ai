package com.applypilot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@org.springframework.context.annotation.Configuration
@EnableConfigurationProperties(AppProperties.class)
public class Configuration {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
