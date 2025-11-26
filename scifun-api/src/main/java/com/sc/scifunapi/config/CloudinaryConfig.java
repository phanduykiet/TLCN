package com.sc.scifunapi.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(
            @Value("${cloudinary.cloud_name}") String cloud,
            @Value("${cloudinary.api_key}") String key,
            @Value("${cloudinary.api_secret}") String secret
    ) {
        return new Cloudinary(Map.of(
                "cloud_name", cloud,
                "api_key", key,
                "api_secret", secret
        ));
    }
}
