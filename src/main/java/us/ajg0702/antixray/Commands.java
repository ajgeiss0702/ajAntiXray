package us.ajg0702.antixray;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.ajg0702.utils.common.Messages;

import java.util.Map;

public class Commands implements CommandExecutor {
    private final Main plugin;

    private final LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.legacySection();

    public Commands(Main plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender bsender, Command command, String label, String[] args) {
        Audience sender = plugin.adventure().sender(bsender);
        if(label.equalsIgnoreCase("ajecho")) {
            StringBuilder message = new StringBuilder();
            for(String arg : args) {
                message.append(arg).append(" ");
            }
            sender.sendMessage(plugin.getMessages().toComponent(Messages.color(message.toString())));
            return true;
        }
        if(args.length == 1) {
            if(!args[0].equalsIgnoreCase("reload")) {
                if(!bsender.hasPermission("ajaxr.check")) {
                    sender.sendMessage(plugin.getMessages().getComponent("noperm"));
                    return true;
                }
                if(Bukkit.getPlayer(args[0]) == null) {
                    sender.sendMessage(
                            plugin.getMessages().getComponent("player-not-found", "PLAYER:", args[0])
                    );
                    return true;
                }
                Player p = Bukkit.getPlayer(args[0]);
                Component add = plugin.getMessages().getComponent("get.header", "PLAYER:"+p.getName());
                add = add.append(Component.newline());
                Map<String, Integer> bks = plugin.getBlocks(p.getUniqueId());
                for(String block : bks.keySet()) {
                    int blocknum = bks.get(block);
                    int blockmax = plugin.warnBlocks.get(block);
                    String countcolor;
                    if(blocknum >= blockmax) {
                        if(blocknum > blockmax + blockmax*0.25) {
                            countcolor = "<dark_red>";
                        } else {
                            countcolor = "<red>";
                        }
                    } else {
                        if(blocknum > blockmax - blockmax*0.2) {
                            countcolor = "<yellow>";
                        } else if (blocknum > blockmax - blockmax*0.35) {
                            countcolor = "<dark_green>";
                        } else {
                            countcolor = "<green>";
                        }
                    }
                    add = add.append(plugin.getMessages().getComponent(
                            "get.format",
                            "BLOCK:" + block,
                            "COUNTCOLOR:" + countcolor,
                            "COUNT:" + blocknum,
                            "DELAY:" + (plugin.delay / 60000)
                    ));
                    add = add.append(Component.newline());

                }
                sender.sendMessage(add);
            } else {
                if(!bsender.hasPermission("ajaxr.reload")) {
                    sender.sendMessage(plugin.getMessages().getComponent("noperm"));
                    return true;
                }
                plugin.reloadMainConfig();
                plugin.getMessages().reload();
                sender.sendMessage(plugin.getMessages().getComponent("config-reloaded"));
            }
            return true;
        }

        sender.sendMessage(plugin.getMessages().getComponent("cmd-syntax", "CMD:"+label));

        return true;
    }
}
