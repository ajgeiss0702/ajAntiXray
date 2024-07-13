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
        Location blockloc = e.getBlock().getLocation();

        if(plugin.disabledWorlds.contains(blockloc.getWorld().getName())) return;
        if(blockloc.getY() > plugin.ignoreAbove) return;

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
            if(!hook.isEnabled()) continue;
            if(!hook.check(e.getPlayer(), blockloc)) return;
        }

        Map<Long, String> player = plugin.players.get(e.getPlayer().getUniqueId());
        if(player == null) {
            player = new HashMap<>();
        }
        player.put(new Date().getTime(), block);
        plugin.players.put(e.getPlayer().getUniqueId(), player);


        UUID uuid = e.getPlayer().getUniqueId();
        if(plugin.lastNotify.containsKey(e.getPlayer().getUniqueId())) {
            //Bukkit.getLogger().info("has key");
            Long ln = plugin.lastNotify.get(uuid);
            if(new Date().getTime() - ln >= 30e3) {
                //Bukkit.getLogger().info("time good " + (new Date().getTime() - ln)/1000);
                plugin.notifyAdmins(e.getPlayer());
            }
        } else {
            //Bukkit.getLogger().info("no key");
            plugin.notifyAdmins(e.getPlayer());
        }

        if(e.getPlayer().hasPermission("ajaxr.debug") && plugin.blockDebug) {
            adventurePlayer.sendMessage(plugin.getMessages().toComponent("<green>"+block));
        }

    }
}
