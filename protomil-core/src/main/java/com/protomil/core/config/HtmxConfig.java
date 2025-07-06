package com.protomil.core.config;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class HtmxConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/wireframes/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .exposedHeaders("HX-Trigger", "HX-Location", "HX-Push-Url", "HX-Redirect", "HX-Refresh");
    }
}