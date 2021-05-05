package com.teamz.chatapp.pockafka.websocket;

import com.teamz.messages.*;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Controller
public class ChatController {
    private Map<String, List<PartitionInfo>> topics;
    private List<String> channels = new LinkedList<>();

    @Autowired
    private KafkaTemplate<String, ChatMessage> kafkaTemplate;

    private KafkaConsumer<String, String> consumer;

    @MessageMapping("/chat/{channel}")
    public void send(@DestinationVariable String channel, final ChatMessage message) {
        try {
            // Sending the message to kafka topic queue
            kafkaTemplate.send("chat_" + channel, message).get();
            System.out.println("Message sent");

            if (!channels.contains(message.getChannel())) {
                sendProc("chat_" + channel);
                channels.add(channel);
            }
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendProc(String chat) throws IOException {
        StringBuilder str = new StringBuilder();
        str.append("{\"name\":\"datastax-connector-");
        str.append(chat);
        str.append("-table\",\"config\":{\"connector.class\":\"com.datastax.oss.kafka.sink.CassandraSinkConnector\",\"loadBalancing.localDc\":\"datacenter1\",\"topic.");
        str.append(chat);
        str.append(".kafka_examples.chats.mapping\":\"sender=value.sender, message=value.message, date_time=value.datetime, channel=value.channel\",\"tasks.max\":\"10\",\"topics\":\"");
        str.append(chat);
        str.append("\",\"contactPoints\":\"cassandra\",\"topic.");
        str.append(chat);
        str.append(".kafka_examples.chats.consistencyLevel\":\"LOCAL_QUORUM\"}}");

        String data = str.toString();
        ProcessBuilder pb = new ProcessBuilder("curl",
        "-vvv",
        "-X",
        "POST",
        "-H",
        "Content-Type:application/json",
        "-d",
        data,
        "http://datastax-connect:8083/connectors");
        // errorstream of the process will be redirected to standard output
        pb.redirectErrorStream(true);
        // start the process
        Process proc = pb.start();
        /*
         * get the inputstream from the process which would get printed on the console /
         * terminal
         */
        InputStream ins = proc.getInputStream();
        // creating a buffered reader
        BufferedReader read = new BufferedReader(new InputStreamReader(ins));
        StringBuilder sb = new StringBuilder();
        read.lines().forEach(line -> {
            System.out.println("line>" + line);
            sb.append(line);
        });
        // close the buffered reader
        read.close();
        /*
         * wait until process completes, this should be always after the input_stream of
         * processbuilder is read to avoid deadlock situations
         */
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        /* exit code can be obtained only after process completes, 0 
        * indicates a successful completion
        */
        int exitCode = proc.exitValue();
        System.out.println("exit code::"+exitCode);
        // finally destroy the process
        proc.destroy();
    }

    @MessageMapping("/channels")
    @SendTo("/topic/channels")
    public ChannelsMessage send() {
        if (consumer == null) {
            Properties props = new Properties();
            props.put("bootstrap.servers", "kafka2:9092");
            props.put("group.id", "kafka-sandbox");
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            consumer = new KafkaConsumer<>(props);
        }
        topics = consumer.listTopics();
        for(Map.Entry<String, List<PartitionInfo>> entry : topics.entrySet()) {
            String topic = entry.getValue().get(0).topic();
            if(!channels.contains(topic.substring(5)) && topic.contains("chat_"))
                channels.add(topic.substring(5));
        }
        return new ChannelsMessage(channels);
    }
}
