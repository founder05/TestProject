package me.marcdoesntexists.nations.listeners;

import org.bukkit.event.player.AsyncPlayerChatEvent;
import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.ChatManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Alliance;
import me.marcdoesntexists.nations.societies.Empire;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Religion;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import me.marcdoesntexists.nations.utils.MessageUtils;

import java.util.Map;
import java.util.UUID;

public class ChatChannelListener implements Listener {
    private final Nations plugin;
    private final ChatManager chatManager = ChatManager.getInstance();
    private final SocietiesManager societies = SocietiesManager.getInstance();

    public ChatChannelListener(Nations plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        UUID senderId = sender.getUniqueId();

        // Determine channel: player preference from PlayerData or ChatManager fallback
        PlayerData pd = plugin.getDataManager().getPlayerData(senderId);
        ChatManager.Channel channel = chatManager.getChannel(senderId);

        event.setCancelled(true);

        String formatted = MessageUtils.format("chat.format", Map.of("channel", channel.name(), "player", sender.getName(), "message", event.getMessage()));

        switch (channel) {
            case GLOBAL:
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(formatted));
                break;
            case TOWN: {
                if (pd == null || pd.getTown() == null) {
                    sender.sendMessage(MessageUtils.get("chat.not_in_town"));
                    return;
                }
                Town town = societies.getTown(pd.getTown());
                if (town == null) {
                    sender.sendMessage(MessageUtils.get("chat.town_data_not_found"));
                    return;
                }
                for (UUID member : town.getMembers()) {
                    Player p = Bukkit.getPlayer(member);
                    if (p != null && p.isOnline()) p.sendMessage(formatted);
                }
                break;
            }
            case KINGDOM: {
                if (pd == null || pd.getTown() == null) {
                    sender.sendMessage(MessageUtils.get("chat.not_in_town"));
                    return;
                }
                Town town = societies.getTown(pd.getTown());
                if (town == null) { sender.sendMessage(MessageUtils.get("chat.town_data_not_found")); return; }
                String kingdomName = town.getKingdom();
                if (kingdomName == null) { sender.sendMessage(MessageUtils.get("chat.not_in_kingdom")); return; }
                Kingdom kingdom = societies.getKingdom(kingdomName);
                if (kingdom == null) { sender.sendMessage(MessageUtils.get("chat.kingdom_data_not_found")); return; }

                for (String townName : kingdom.getTowns()) {
                    Town t = societies.getTown(townName);
                    if (t == null) continue;
                    for (UUID member : t.getMembers()) {
                        Player p = Bukkit.getPlayer(member);
                        if (p != null && p.isOnline()) p.sendMessage(formatted);
                    }
                }
                break;
            }
            case EMPIRE: {
                if (pd == null || pd.getTown() == null) {
                    sender.sendMessage(MessageUtils.get("chat.not_in_town"));
                    return;
                }
                Town town = societies.getTown(pd.getTown());
                if (town == null) { sender.sendMessage(MessageUtils.get("chat.town_data_not_found")); return; }
                String kingdomName = town.getKingdom();
                if (kingdomName == null) { sender.sendMessage(MessageUtils.get("chat.not_in_kingdom")); return; }
                Kingdom kingdom = societies.getKingdom(kingdomName);
                if (kingdom == null) { sender.sendMessage(MessageUtils.get("chat.kingdom_data_not_found" )); return; }
                String empireName = kingdom.getEmpire();
                if (empireName == null) { sender.sendMessage(MessageUtils.get("chat.not_in_empire" )); return; }
                Empire empire = societies.getEmpire(empireName);
                if (empire == null) { sender.sendMessage(MessageUtils.get("chat.empire_data_not_found" )); return; }

                for (String k : empire.getKingdoms()) {
                    Kingdom kObj = societies.getKingdom(k);
                    if (kObj == null) continue;
                    for (String townName : kObj.getTowns()) {
                        Town tt = societies.getTown(townName);
                        if (tt == null) continue;
                        for (UUID member : tt.getMembers()) {
                            Player p = Bukkit.getPlayer(member);
                            if (p != null && p.isOnline()) p.sendMessage(formatted);
                        }
                    }
                }
                break;
            }
            case RELIGION: {
                if (pd == null || pd.getReligion() == null) { sender.sendMessage(MessageUtils.get("chat.not_in_religion")); return; }
                Religion religion = societies.getReligion(pd.getReligion());
                if (religion == null) { sender.sendMessage(MessageUtils.get("chat.religion_data_not_found" )); return; }
                for (UUID follower : religion.getFollowers()) {
                    Player p = Bukkit.getPlayer(follower);
                    if (p != null && p.isOnline()) p.sendMessage(formatted);
                }
                break;
            }
            case ALLIANCE: {
                if (pd == null || pd.getTown() == null) { sender.sendMessage(MessageUtils.get("chat.not_in_town")); return; }
                Town townObj = societies.getTown(pd.getTown());
                if (townObj == null) { sender.sendMessage(MessageUtils.get("chat.town_data_not_found" )); return; }
                String playerKingdom = townObj.getKingdom();
                if (playerKingdom == null) { sender.sendMessage(MessageUtils.get("chat.not_in_kingdom" )); return; }

                // iterate alliances and find those that include the player's kingdom
                for (Alliance alliance : societies.getAlliances()) {
                    if (!alliance.getMembers().contains(playerKingdom)) continue;
                    // send to all members of all member kingdoms
                    for (String memberKingdom : alliance.getMembers()) {
                        Kingdom kObj = societies.getKingdom(memberKingdom);
                        if (kObj == null) continue;
                        for (String townName : kObj.getTowns()) {
                            Town t = societies.getTown(townName);
                            if (t == null) continue;
                            for (UUID member : t.getMembers()) {
                                Player p = Bukkit.getPlayer(member);
                                if (p != null && p.isOnline()) p.sendMessage(formatted);
                            }
                        }
                    }
                }
                break;
            }
        }

        // send to spies (admins) who have spy enabled
        for (UUID adminId : chatManager.getSpyAdmins()) {
            Player p = Bukkit.getPlayer(adminId);
            if (p != null && p.isOnline()) p.sendMessage(MessageUtils.get("chat.spy_prefix") + formatted);
        }
    }
}
