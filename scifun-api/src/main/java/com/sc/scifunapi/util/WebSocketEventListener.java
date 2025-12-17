package com.sc.scifunapi.util;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();
        String userId = accessor.getUser() != null
                ? accessor.getUser().getName()
                : "anonymous";

        System.out.println("✅ WebSocket CONNECTED | sessionId="
                + sessionId + " | userId=" + userId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        System.out.println("❌ WebSocket DISCONNECTED | sessionId="
                + event.getSessionId());
    }
}
