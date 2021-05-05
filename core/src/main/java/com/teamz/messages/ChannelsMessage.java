package com.teamz.messages;

import java.util.List;

public class ChannelsMessage {
    private List<String> channels;

    public ChannelsMessage() {}
    public ChannelsMessage(List<String> channels) {
        this.channels = channels;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }
}
