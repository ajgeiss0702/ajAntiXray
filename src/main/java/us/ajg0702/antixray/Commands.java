package us.ajg0702.antixray;

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
    private final Messages msgs;

    public Commands(Main plugin) {
        this.plugin = plugin;
        this.msgs = plugin.getMessages();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(label.equalsIgnoreCase("ajecho")) {
            StringBuilder message = new StringBuilder();
            for(String arg : args) {
                message.append(arg).append(" ");
            }
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message.toString()));
            return true;
        }
        if(args.length == 1) {
				/*if(args[0].equalsIgnoreCase("debug")) {
					if(checkWG) {
						sender.sendMessage(WGManager.getInstance().check(((Player) sender).getLocation())+"");
					}
					return true;
				}*/
            if(!args[0].equalsIgnoreCase("reload")) {
                if(!sender.hasPermission("ajaxr.check")) {
                    sender.sendMessage(msgs.getMMString("noperm"));
                    return true;
                }
                if(Bukkit.getPlayer(args[0]) == null) {
                    sender.sendMessage(msgs.getMMString("player-not-found").replaceAll("\\{PLAYER\\}", args[0]));
                    return true;
                }
                Player p = (Player) Bukkit.getPlayer(args[0]);

                String add = msgs.getMMString("get.header").replaceAll("\\{PLAYER\\}", p.getName())+"\n";
                Map<String, Integer> bks = plugin.getBlocks(p.getUniqueId());
                for(String block : bks.keySet()) {
                    int blocknum = bks.get(block);
                    int blockmax = plugin.warnBlocks.get(block);
                    String countcolor = "a";
                    if(blocknum >= blockmax) {
                        if(blocknum > blockmax + blockmax*0.25) {
                            countcolor = "4";
                        } else {
                            countcolor = "c";
                        }
                    } else {
                        if(blocknum > blockmax - blockmax*0.2) {
                            countcolor = "e";
                        } else if (blocknum > blockmax - blockmax*0.35) {
                            countcolor = "2";
                        } else {
                            countcolor = "a";
                        }
                    }
                    add += msgs.getMMString("get.format")
                            .replaceAll("\\{BLOCK\\}", block)
                            .replaceAll("\\{COUNTCOLOR\\}", "§"+countcolor)
                            .replaceAll("\\{COUNT\\}", blocknum+"")
                            .replaceAll("\\{DELAY\\}", (plugin.delay/60000)+"") + "\n";

                }
                sender.sendMessage(add);
                return true;
            } else {
                if(!sender.hasPermission("ajaxr.reload")) {
                    sender.sendMessage(msgs.getMMString("noperm"));
                    return true;
                }
                plugin.reloadMainConfig();
                msgs.reload();
                sender.sendMessage(msgs.getMMString("config-reloaded"));
                return true;
            }
        }

        sender.sendMessage(msgs.getMMString("cmd-syntax").replaceAll("\\{CMD\\}", label));

        return true;
    }
}
