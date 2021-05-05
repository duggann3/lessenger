package com.teamz.chatapp.pockafka.kafka;


import com.teamz.messages.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {
    @Autowired
    SimpMessagingTemplate template;

    @KafkaListener(
            topicPattern = "chat_.*",
            groupId = "kafka-sandbox"
    )
    public void listen(ChatMessage message) {
        System.out.println("Sending via kafka listener..");
        template.convertAndSend("/topic/" + message.getChannel(), message);
    }
}