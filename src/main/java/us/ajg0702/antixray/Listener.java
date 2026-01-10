package us.ajg0702.antixray;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import us.ajg0702.antixray.hooks.Hook;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Listener implements org.bukkit.event.Listener {
    private final Main plugin;

    public Listener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // CHANGED (Folia): Safe because plugin.players is now a ConcurrentHashMap in Main
        plugin.players.remove(e.getPlayer().getUniqueId());

        // Optional: also clear notify cooldown so the map doesn't grow forever
        // CHANGED (Folia): lastNotify is also ConcurrentHashMap in Main
        plugin.lastNotify.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBreakBlock(BlockBreakEvent e) {
        // CHANGED (Folia): Do NOT hop to the global thread here.
        // BlockBreakEvent already runs on the correct region thread for the block.
        // If we moved this to GlobalRegionScheduler, we'd risk unsafe world access.

        final Player breaker = e.getPlayer();
        final Location loc = e.getBlock().getLocation();

        // NEW: exempt players never get tracked or notified
        if (breaker.hasPermission("ajaxr.exempt")) return;

        String block = e.getBlock().getType().toString();

        // Defensive null check (rare, but protects against weird worlds unloading)
        if (loc.getWorld() == null) return;

        if (plugin.disabledWorlds.contains(loc.getWorld().getName())) return;
        if (loc.getY() > plugin.ignoreAbove) return;

        if (block.startsWith("DEEPSLATE_") && plugin.getAConfig().getBoolean("merge-deepslate")) {
            block = block.substring(10);
        }

        if (!plugin.blocks.contains(block)) {
            if (plugin.blockDebug && breaker.hasPermission("ajaxr.debug")) {
                final String msgBlock = block;
                // CHANGED (Folia): Schedule message via the player's scheduler (safe regardless of calling thread)
                breaker.getScheduler().run(plugin,
                        t -> breaker.sendMessage(plugin.getMessages().toComponent("<red>" + msgBlock)),
                        null
                );
            }
            return;
        }

        // Hooks are executed on the same region thread as the block break (safe for world checks)
        for (Hook hook : plugin.getHookRegistry().getHooks()) {
            try {
                if (!hook.isEnabled()) continue;
                if (!hook.check(breaker, loc)) return;
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING,
                        "An error occurred while checking hook " + hook.getClass().getName() + ":",
                        ex
                );
            }
        }

        // CHANGED (Folia): Use a thread-safe per-player map (ConcurrentHashMap) instead of HashMap
        // This matches the updated types in Main:
        //   players: ConcurrentHashMap<UUID, ConcurrentHashMap<Long, String>>
        Map<Long, String> playerMap = plugin.players.computeIfAbsent(
                breaker.getUniqueId(),
                k -> new ConcurrentHashMap<>()
        );
        playerMap.put(System.currentTimeMillis(), block);

        // Notify cooldown: safe because lastNotify is ConcurrentHashMap in Main
        UUID uuid = breaker.getUniqueId();
        Long last = plugin.lastNotify.get(uuid);
        if (last == null || System.currentTimeMillis() - last >= 30_000L) {
            // CHANGED (Folia): notifyAdmins() is responsible for scheduling its own work safely.
            plugin.notifyAdmins(breaker);
        }

        if (breaker.hasPermission("ajaxr.debug") && plugin.blockDebug) {
            final String msgBlock = block;
            breaker.getScheduler().run(plugin,
                    t -> breaker.sendMessage(plugin.getMessages().toComponent("<green>" + msgBlock)),
                    null
            );
        }
    }
}
