package us.ajg0702.antixray;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.ajg0702.utils.common.Messages;

import java.util.Map;

public class Commands implements CommandExecutor {
    private final Main plugin;

    public Commands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender bsender, Command command, String label, String[] args) {
        final CommandSender sender = bsender;

        //Get command name and check if it has 0 arguments, echo back the command
        if (command.getName().equalsIgnoreCase("ajecho")){
            if (args.length == 0){
                sender.sendMessage(plugin.getMessages().getComponent("cmd-syntax", "CMD:" + label));
                return true;
            }
            String message = String.join(" ", args);
            sender.sendMessage(plugin.getMessages().toComponent(Messages.color(message)));
            return true;
        }

        if(args.length == 1) {

            // ajaxr reload
            if (args[0].equalsIgnoreCase("reload"))
            {
                if (!sender.hasPermission("ajaxr.reload"))
                {
                    sender.sendMessage(plugin.getMessages().getComponent("noperm"));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getMessages().reload();
                sender.sendMessage(plugin.getMessages().getComponent("config-reloaded"));
                return true;
            }

            // ajaxr check <player>
            if (!sender.hasPermission("ajaxr.check")) {
                sender.sendMessage(plugin.getMessages().getComponent("noperm"));
                return true;
            }

            //the player we are checking MUST be online
            Player p = Bukkit.getPlayerExact(args[0]); // exact match player name to avoid surprises
            if (p == null) {
                sender.sendMessage(plugin.getMessages().getComponent("player-not-found", "PLAYER:", args[0]));
                return true;
            }

            //Start building the message header
            Component out = plugin.getMessages().getComponent("get.header", "PLAYER:", p.getName())
                    .append(Component.newline());

            //Get blocks that we are tracking
            Map<String, Integer> bks = plugin.getBlocks(p.getUniqueId());

            for (Map.Entry<String, Integer> entry : bks.entrySet()) {
                String block = entry.getKey();
                int blocknum = entry.getValue();

                int blockmax = plugin.warnBlocks.getOrDefault(block, 0);
                if (blockmax <= 0) continue;

                String countcolor;
                if (blocknum >= blockmax) {
                    countcolor = (blocknum > blockmax + (int) (blockmax * 0.25)) ? "<dark_red>" : "<red>";
                } else {
                    if (blocknum > blockmax - (int) (blockmax * 0.2)) {
                        countcolor = "<yellow>";
                    } else if (blocknum > blockmax - (int) (blockmax * 0.35)) {
                        countcolor = "<dark_green>";
                    } else {
                        countcolor = "<green>";
                    }
                }

                out = out.append(plugin.getMessages().getComponent(
                        "get.format",
                        "BLOCK:" + block,
                        "COUNTCOLOR:" + countcolor,
                        "COUNT:" + blocknum,
                        "DELAY:" + (plugin.delay / 60000)
                )).append(Component.newline());
            }

            //Display the report
            sender.sendMessage(out);
            return true;
        }

        //Display syntax/help
        sender.sendMessage(plugin.getMessages().getComponent("cmd-syntax", "CMD:", label));
        return true;
    }
}

// Old Stuff
            /*
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
*/