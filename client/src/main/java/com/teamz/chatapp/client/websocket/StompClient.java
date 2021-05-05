package com.teamz.chatapp.client.websocket;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

public class StompClient {
    public WebSocketClient client;
    public WebSocketStompClient stompClient;

    public StompSessionHandler sessionHandler;
    public StompSession session;

    public StompClient(String URL) {
        client = new StandardWebSocketClient();
        stompClient = new WebSocketStompClient(client);

        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        try {
            sessionHandler = new MyStompSessionHandler();
            session = stompClient.connect(URL, sessionHandler).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
