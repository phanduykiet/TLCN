package com.sc.scifunapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${client.url:http://localhost:3000}")
    private String clientUrl;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // giống base URL của Socket.IO
                .setAllowedOrigins(clientUrl)
                .withSockJS(); // enable fallback giống "polling"
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // frontend subscribes vào
        registry.enableSimpleBroker("/topic", "/queue");

        // frontend send message đến backend
        registry.setApplicationDestinationPrefixes("/app");
    }
}
