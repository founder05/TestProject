package me.marcdoesntexists.nations.managers;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager {
    private static ChatManager instance;
    private final Map<UUID, Channel> playerChannel = new ConcurrentHashMap<>();
    private final Set<UUID> spyAdmins = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private ChatManager() {
    }

    public static synchronized ChatManager getInstance() {
        if (instance == null) instance = new ChatManager();
        return instance;
    }

    public void setChannel(UUID playerId, Channel channel) {
        playerChannel.put(playerId, channel);
    }

    public Channel getChannel(UUID playerId) {
        return playerChannel.getOrDefault(playerId, Channel.GLOBAL);
    }

    public void addSpy(UUID adminId) {
        spyAdmins.add(adminId);
    }

    public void removeSpy(UUID adminId) {
        spyAdmins.remove(adminId);
    }

    public boolean isSpying(UUID adminId) {
        return spyAdmins.contains(adminId);
    }

    public Set<UUID> getSpyAdmins() {
        return Collections.unmodifiableSet(spyAdmins);
    }

    public enum Channel {
        GLOBAL, TOWN, KINGDOM, EMPIRE, RELIGION, ALLIANCE
    }
}

