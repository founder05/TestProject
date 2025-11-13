package me.marcdoesntexists.nations.managers;

import org.bukkit.entity.Player;

/**
 * Lightweight interface for claim visualizers. Provide two implementations:
 * - ProtocolLibClaimVisualizer (uses ProtocolLib + BlockDisplay packets)
 * - ParticleClaimVisualizer (fallback using particles)
 */
public interface ClaimVisualizer {
    boolean isVisualizing(Player player);

    void toggleVisualization(Player player, String townName);

    void startVisualization(Player player, String townName);

    void stopVisualization(Player player);

    void stopAll();
}
