// src/main/java/com/sc/scifunapi/config/MethodSecurityConfig.java
package com.sc.scifunapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity // cho phép dùng @PreAuthorize trên controller/service
public class MethodSecurityConfig {}
