package com.sc.scifunapi.middleware;

import com.sc.scifunapi.dto.user.JwtService;
import com.sc.scifunapi.dto.user.JwtUser;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String auth = accessor.getFirstNativeHeader("Authorization"); // Bearer <token>
            if (auth == null || !auth.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing Authorization header");
            }

            String token = auth.substring(7);
            JwtUser u = jwtService.verify(token);

            // Principal name = userId (để /user/queue hoạt động theo userId)
            accessor.setUser(new UsernamePasswordAuthenticationToken(u.userId(), null, List.of()));

            // Nếu cần role/email trong session:
            accessor.getSessionAttributes().put("role", u.role());
            accessor.getSessionAttributes().put("email", u.email());
        }

        return message;
    }
}
