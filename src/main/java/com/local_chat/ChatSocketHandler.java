package com.local_chat;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import tools.jackson.databind.ObjectMapper;

@Component
public class ChatSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper = new ObjectMapper();

    ChatService service;
    public ChatSocketHandler(ChatService service) { this.service = service; }

    @Override
    public void afterConnectionEstablished(WebSocketSession raw) {
        WebSocketSession session = new ConcurrentWebSocketSessionDecorator(raw, 5_000, 64 * 1024);
        String username = raw.getPrincipal().getName();
        service.addConnection(username, session);
    }

    private record Message(String chat_name, String content) {}; 
    @Override
    protected void handleTextMessage(WebSocketSession raw, TextMessage message) throws IOException {
        String username = raw.getPrincipal().getName();
        var msg = mapper.readValue(message.getPayload(), Message.class);
        if (msg.content() == null || msg.chat_name == null) return;
        this.service.sendMessage(username, msg.content(), msg.chat_name);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession raw, CloseStatus status) {
        String username = raw.getPrincipal().getName();
        this.service.closeConnection(username, raw);
    }

    public void closeAllFor(String username) {
        this.service.logout(username);
    }
}
