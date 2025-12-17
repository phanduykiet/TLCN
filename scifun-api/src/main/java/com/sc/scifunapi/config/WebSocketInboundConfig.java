package com.sc.scifunapi.config;

import com.sc.scifunapi.middleware.StompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketInboundConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor interceptor;

    public WebSocketInboundConfig(StompAuthChannelInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(interceptor);
    }
}
