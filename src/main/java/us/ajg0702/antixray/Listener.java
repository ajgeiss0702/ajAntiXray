package us.ajg0702.antixray;

import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import us.ajg0702.antixray.hooks.Hook;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class Listener implements org.bukkit.event.Listener {
    private final Main plugin;

    public Listener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.players.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBreakBlock(BlockBreakEvent e) {

        Audience adventurePlayer = plugin.adventure().player(e.getPlayer());

        String block = e.getBlock().getType().toString();
        Location blockLocation = e.getBlock().getLocation();

        if(plugin.disabledWorlds.contains(blockLocation.getWorld().getName())) return;
        if(blockLocation.getY() > plugin.ignoreAbove) return;

        if(block.startsWith("DEEPSLATE_") && plugin.getAConfig().getBoolean("merge-deepslate")) {
            block = block.substring(10);
        }

        if(!plugin.blocks.contains(block)) {
            if(plugin.blockDebug && e.getPlayer().hasPermission("ajaxr.debug")) {
                adventurePlayer.sendMessage(plugin.getMessages().toComponent("<red>"+block));
            }
            return;
        }


        for(Hook hook : plugin.getHookRegistry().getHooks()) {
            try {
                if(!hook.isEnabled()) continue;
                if(!hook.check(e.getPlayer(), blockLocation)) return;
            } catch(Exception ex) {
                plugin.getLogger().log(Level.WARNING, "An error occurred while checking hook " + hook.getClass().getName() + ":", ex);
            }
        }

        Map<Long, String> player = plugin.players.computeIfAbsent(e.getPlayer().getUniqueId(), k -> new HashMap<>());
        player.put(System.currentTimeMillis(), block);
        plugin.players.put(e.getPlayer().getUniqueId(), player);


        UUID uuid = e.getPlayer().getUniqueId();
        if(plugin.lastNotify.containsKey(e.getPlayer().getUniqueId())) {
            Long ln = plugin.lastNotify.get(uuid);
            if(System.currentTimeMillis() - ln >= 30e3) {
                plugin.notifyAdmins(e.getPlayer());
            }
        } else {
            plugin.notifyAdmins(e.getPlayer());
        }

        if(e.getPlayer().hasPermission("ajaxr.debug") && plugin.blockDebug) {
            adventurePlayer.sendMessage(plugin.getMessages().toComponent("<green>"+block));
        }

    }
}
