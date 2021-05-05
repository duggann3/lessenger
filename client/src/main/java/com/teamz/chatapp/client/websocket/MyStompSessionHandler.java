package com.teamz.chatapp.client.websocket;

import com.teamz.chatapp.client.Main;
import com.teamz.messages.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.lang.reflect.Type;


public class MyStompSessionHandler extends StompSessionHandlerAdapter {
    private Logger logger = LogManager.getLogger(MyStompSessionHandler.class);

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        logger.info("New session established: " + session.getSessionId());
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        logger.error("Got an exception", exception);
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        if (headers.getDestination().contains("channels"))
            return ChannelsMessage.class;
        else
            return ChatMessage.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        try {
            ChatMessage msg = (ChatMessage) payload;
            logger.info("Received : " + msg.getMessage() + " from : " + msg.getSender());
            Main.addMessage(msg);
            return;
        } catch (ClassCastException ignored) {}
        try {
            ChannelsMessage msg = (ChannelsMessage) payload;
            logger.info ("Received:" + msg.getChannels().toString());
            Main.channels = msg.getChannels();
        } catch (ClassCastException ignored) {}
    }
}
