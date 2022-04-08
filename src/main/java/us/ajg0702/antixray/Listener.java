package us.ajg0702.antixray;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBreakBlock(BlockBreakEvent e) {

        String block = e.getBlock().getType().toString();
        Location blockloc = e.getBlock().getLocation();

        if(plugin.disabledWorlds.contains(blockloc.getWorld().getName())) return;
        if(blockloc.getY() > plugin.ignoreAbove) return;

        if(!plugin.blocks.contains(block)) {
            if(e.getPlayer().hasPermission("ajaxr.debug") && plugin.blockDebug) {
                e.getPlayer().sendMessage("§c"+block);
            }
            return;
        }


        for(Hook hook : plugin.getHookRegistry().getHooks()) {
            if(!hook.isEnabled()) continue;
            if(!hook.check(e.getPlayer(), blockloc)) return;
        }

        Map<Long, String> player = plugin.players.get(e.getPlayer().getUniqueId());
        if(player == null) {
            player = new HashMap<Long, String>();
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
            e.getPlayer().sendMessage("§a"+block);
        }

    }
}
