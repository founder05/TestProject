package me.marcdoesntexists.nations.listeners;

import me.marcdoesntexists.nations.events.CrimeCommittedEvent;
import me.marcdoesntexists.nations.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CrimeBroadcastListener implements Listener {

    @EventHandler
    public void onCrimeCommitted(CrimeCommittedEvent event) {
        if (!event.shouldProsecute()) return; // ignore crimes that are not prosecutable

        String town = event.getTownName();
        String type = event.getCrimeType().getDisplayName();
        String criminal = event.getCriminal() != null ? event.getCriminal().getName() : event.getCriminalId().toString();
        String coords = "";
        if (event.getLocation() != null) {
            coords = event.getLocation().getWorld().getName() + "," + event.getLocation().getBlockX() + "," + event.getLocation().getBlockY() + "," + event.getLocation().getBlockZ();
        }
        String msg = MessageUtils.format("justice.broadcast", java.util.Map.of("town", town, "type", type, "criminal", criminal, "coords", coords));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("nations.notice.crime")) {
                p.sendMessage(msg);
            }
        }
    }
}
