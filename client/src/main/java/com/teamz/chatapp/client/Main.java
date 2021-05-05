package com.teamz.chatapp.client;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.TerminalResizeListener;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import com.teamz.chatapp.client.websocket.StompClient;
import com.teamz.messages.*;
import org.springframework.messaging.simp.stomp.StompSession;

import javax.swing.*;

public class Main {
    public static Terminal terminal = null;
    public static TextGraphics textGraphics;
    static TerminalState terminalState;
    static StringBuilder sb = new StringBuilder();

    // Use this URL to connect to a remote server we have set up, and test with other people
    final static String SERVER_URL = "ws://144.91.108.78:8090/chat";
    // Use this URL to test with the server that you have created locally
    //final static String SERVER_URL = "ws://localhost:8080/chat";

    static volatile int lastRow;
    static String userName;
    static String currentChannel;
    public static volatile List<String> channels;
    public static volatile List<ChatMessage> storedMessages;

    static StompClient serverConnection;
    static StompSession.Subscription subscription;

    // Listener to adjust the position of text according to window size
    static TerminalResizeListener resizeListener = (terminal, terminalSize) -> {
        try {
            lastRow = terminalSize.getRows();
            terminal.setCursorPosition(terminal.getCursorPosition().getColumn(), lastRow);
            if (terminalState == TerminalState.USERNAME) {
                showUsername();
                textGraphics.putString(1, lastRow - 1, "> ");
                textGraphics.putString(3, lastRow - 1, sb.toString());
                terminal.flush();
            }
            if (terminalState == TerminalState.CHANNEL_LIST) {
                showChannels();
                textGraphics.putString(1, lastRow - 1, "> ");
                textGraphics.putString(3, lastRow - 1, sb.toString());
                terminal.flush();
            }
            if (terminalState == TerminalState.IN_CHANNEL) {
                scrollMessages(0);
                terminal.setCursorPosition(Math.max(sb.length(), 3), lastRow);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    public static void main(String[] args) {
        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
        try {
            // Initial setup
            defaultTerminalFactory.setTerminalEmulatorTitle("Lessenger");
            terminal = defaultTerminalFactory.createTerminal();
            terminal.addResizeListener(resizeListener);
            if (terminal instanceof SwingTerminalFrame)
                ((SwingTerminalFrame) terminal).setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            lastRow = terminal.getTerminalSize().getRows();
            textGraphics = terminal.newTextGraphics();
            textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
            textGraphics.setBackgroundColor(TextColor.ANSI.BLACK);
            terminal.setCursorPosition(0, lastRow);

            // Get username and connect to server
            terminalState = TerminalState.USERNAME;
            showUsername();
            userName = getMessage();
            serverConnection = new StompClient(SERVER_URL);

            // Main loop
            while(true) {
                channels = null;
                storedMessages = new LinkedList<>();
                subscription = serverConnection.session.subscribe("/topic/channels", serverConnection.sessionHandler);
                serverConnection.session.send("/app/channels", new ChannelsMessage());
                terminalState = TerminalState.CHANNEL_LIST;
                // Wait until the server responds with a list of channels
                while (channels == null) {}
                showChannels();
                currentChannel = getMessage().toLowerCase();
                subscription.unsubscribe();
                subscription = serverConnection.session.subscribe("/topic/" + currentChannel, serverConnection.sessionHandler);
                populateChannel();
                terminalState = TerminalState.IN_CHANNEL;
                channelChat();
                subscription.unsubscribe();
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if (terminal != null) {
                try {
                    System.out.println("Closing...");
                    terminal.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ===== [USERNAME PAGE] =====

    public static void showUsername() {
        //Prompt user for a display name
        try {
            terminal.clearScreen();
            textGraphics.putString(1, lastRow - 2,
                    "Please enter a username:", SGR.BOLD);
            terminal.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== [CHANNELS PAGE] =====

    public static void showChannels() {
        try {
            terminal.clearScreen();
            textGraphics.putString(0, 0, "Please select a channel to join:", SGR.BOLD);
            int i = 2;
            for (String channel : channels) {
                textGraphics.putString(0, i++, channel);
            }
            terminal.setCursorPosition(Math.max(sb.length(), 3), lastRow);
            terminal.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void populateChannel() {
        try {
            terminal.clearScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(ChatMessage message : storedMessages) {
            addMessage(message);
            if(!storedMessages.contains(message))
                storedMessages.add(message);
        }
        if (!channels.contains(currentChannel)) {
            try {
                textGraphics.putString(1, lastRow - 1, "Creating channel...", SGR.BLINK);
                terminal.flush();
                Thread.sleep(5000);
                terminal.clearScreen();
            } catch(InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void channelChat() {
        String message = "has joined the channel";
        do {
            // This is horrible parsing and will break very easily... oh well!!
            if (message.split(" ")[0].equals("/nick")) {
                String newName = message.substring(6);
                message = userName + " is now " + newName;
                userName = newName;
            }
            String date_time = LocalDateTime.now().toString();
            ChatMessage chatMessage = new ChatMessage(userName, message, currentChannel, date_time);
            serverConnection.session.send("/app/chat/" + currentChannel, chatMessage);
            message = getMessage();
        } while(!message.equals("/quit"));
        // Send a final leave message on quitting the channel
        serverConnection.session.send("/app/chat/" + currentChannel,
                new ChatMessage(userName, "has left the channel", currentChannel, LocalDateTime.now().toString()));
    }

    // ===== [MESSAGE FORMATTING] =====

    public static void addMessage(ChatMessage message) {
        try {
            // Since messages can be over one line, it won't suffice to just print everything to one line
            List<String> wrappedMessage = TerminalTextUtils.getWordWrappedText(
                    terminal.getTerminalSize().getColumns(), message.toString());
            scrollMessages(wrappedMessage.size());
            int j = 0;
            for (String messageLine : wrappedMessage) {
                if(message.getMessage().equals("has joined the channel") || message.getMessage().equals("has left the channel")) {
                    textGraphics.putString(0, lastRow - 1 - wrappedMessage.size() + j++, messageLine, SGR.BOLD);
                }
                else {
                    textGraphics.putString(0, lastRow - 1 - wrappedMessage.size() + j++, messageLine);
                }
            }
            terminal.setCursorPosition(Math.max(sb.length(), 3), lastRow);
            terminal.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        storedMessages.add(message);
    }

    public static void scrollMessages(int lines) {
        try {
            terminal.clearScreen();
            // Draw from most recent message to oldest
            for (int i = storedMessages.size() - 1; i >= 0; i--) {
                ChatMessage message = storedMessages.get(i);
                List<String> wrappedMessage = TerminalTextUtils.getWordWrappedText(
                        terminal.getTerminalSize().getColumns(), message.toString());
                lines += wrappedMessage.size();
                int j = 0;
                for (String messageLine : wrappedMessage) {
                    if(message.getMessage().equals("has joined the channel") || message.getMessage().equals("has left the channel")) {
                        textGraphics.putString(0, lastRow - 1 - lines + j++, messageLine, SGR.BOLD);
                    }
                    else {
                        textGraphics.putString(0, lastRow - 1 - lines + j++, messageLine);
                    }
                }
            }
            textGraphics.putString(1, lastRow - 1, "> ");
            textGraphics.putString(3, lastRow - 1, sb.toString());
            terminal.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===== [GENERAL FUNCTIONS] =====

    public static String getMessage() {
        sb = new StringBuilder();
        try {
            int i = 3;

            textGraphics.putString(1, lastRow - 1, "> ");
            terminal.setCursorPosition(i, lastRow);
            terminal.flush();

            while(true) {
                KeyStroke keyStroke = terminal.readInput();
                while (keyStroke.getKeyType() != KeyType.Enter) {
                    if (keyStroke.getKeyType() == KeyType.Backspace) {
                        if (sb.length() > 0) {
                            sb.setLength(sb.length() - 1);
                            textGraphics.putString(--i, lastRow - 1, " ");
                        }
                    } else {
                        textGraphics.putString(i++, lastRow - 1, String.valueOf(keyStroke.getCharacter()));
                        sb.append(keyStroke.getCharacter());
                    }
                    terminal.setCursorPosition(i, lastRow);
                    terminal.flush();
                    keyStroke = terminal.readInput();
                }
                // Ensure users can't spend whitespace/blank messages
                if (!sb.toString().equals("") && !sb.toString().matches("\\s*"))
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString().trim();
    }
}
