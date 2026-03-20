package com.sc.scifunapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    public void emitRankChangeToUser(String userId, NotificationService.RankChangePayload payload) {
        // TODO: Implement WebSocket / Socket.io / STOMP ở đây nếu cần
    }
}
