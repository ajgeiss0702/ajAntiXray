package us.ajg0702.antixray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;

import net.md_5.bungee.api.ChatColor;

public class Main extends JavaPlugin implements Listener {
	
	Map<UUID, Map<Long, String>> players = new HashMap<UUID, Map<Long, String>>();
	
	List<String> blocks;
	Map<String, Integer> warnBlocks = new HashMap<String, Integer>();
	int delay;
	
	List<String> disabledWorlds;
	
	Messages msgs = new Messages(this);
	
	private Map<String, Integer> getBlocks(UUID uuid) {
		Map<String, Integer> bks = new HashMap<String, Integer>();
		for(String block : blocks) {
			bks.put(block, 0);
		}
		Map<Long, String> player = players.get(uuid);
		if(player == null) {
			player = new HashMap<Long, String>();
		}
		Iterator<Long> i = player.keySet().iterator();
		while(i.hasNext()) {
			 long t = i.next();
			if(t < new Date().getTime()-delay) {
				i.remove();
			} else {
				String bk = player.get(t);
				int before = bks.get(bk);
				bks.put(bk, before+1);
			}
		}
		players.put(uuid, player);
		return bks;
	}
	
	boolean blockDebug;
	
	boolean checkFactions = false;
	boolean savagefac = false;
	
	List<String> commands;
	
	String notifySound = "NONE";
	
	
	@SuppressWarnings("unchecked")
	private void reloadMainConfig() {
		this.reloadConfig();
		List<String> blockstemp = getConfig().getStringList("blocks");
		blocks = new ArrayList<String>();
		warnBlocks = new HashMap<String, Integer>();
		for(String block : blockstemp) {
			String[] parts = block.split(":");
			if(parts.length > 1 && (parts[0] != null || parts[1] != null)) {
				warnBlocks.put(parts[0], Integer.parseInt(parts[1]));
				blocks.add(parts[0]);
			} else {
				Bukkit.getLogger().warning("[ajAntiXray] The block " + block + " does not have a warning amount set! It will not notify admins!");
				warnBlocks.put(block, Integer.MAX_VALUE);
				blocks.add(block);
			}
		}
		delay = getConfig().getInt("blocks-in-last-minutes") * 60000;
		
		
		
		if(!getConfig().isSet("block-debug")) {
			Bukkit.getLogger().warning("[ajAntiXray] Could not find block-debug option in config! Adding it.");
			getConfig().set("block-debug", false);
			saveConfig();
			blockDebug = false;
		} else {
			blockDebug = getConfig().getBoolean("block-debug");
		}
		if(!getConfig().isSet("disabled-worlds")) {
			Bukkit.getLogger().warning("[ajAntiXray] Could not find disabled-worlds option in config! Adding it.");
			getConfig().set("disabled-worlds", Arrays.asList("example-world"));
			saveConfig();
			disabledWorlds = (List<String>) this.getConfig().getList("disabled-worlds");
		} else {
			disabledWorlds = (List<String>) this.getConfig().getList("disabled-worlds");
		}
		
		if(!getConfig().isSet("commands-to-execute")) {
			Bukkit.getLogger().warning("[ajAntiXray] Could not find command list in config! Adding it.");
			List<String> cmds = new ArrayList<String>();
			cmds.add("ajecho &c{PLAYER} mined {COUNT} {ORE}s in the past {DELAY} minutes [remove/change this command in the config]");
			getConfig().set("commands-to-execute", cmds);
			commands = cmds;
			saveConfig();
		} else {
			commands = getConfig().getStringList("commands-to-execute");
		}
		
		if(!getConfig().isSet("factions-integration")) {
			Bukkit.getLogger().warning("[ajAntiXray] Could not find factions-integration in config! Adding it.");
			getConfig().set("factions-integration", true);
			saveConfig();
			checkFactions = true;
		} else {
			checkFactions = getConfig().getBoolean("factions-integration");
		}
		
		if(!getConfig().isSet("notify-sound")) {
			Bukkit.getLogger().warning("[ajAntiXray] Could not find notify-sound in config! Adding it.");
			getConfig().set("notify-sound", "NONE");
			notifySound = "NONE";
			saveConfig();
		} else {
			notifySound = getConfig().getString("notify-sound");
		}
		
		
		
		if(checkFactions && Bukkit.getPluginManager().getPlugin("Factions") == null) {
			checkFactions = false;
		} else if(checkFactions) {
			Bukkit.getLogger().info("[ajAntiXray] Factions integration enabled.");
		}
	}
	
	Metrics stats;
	@Override
	public void onEnable() {
		
		try {
			stats = new Metrics(this);
		} catch (Exception e) {
			Bukkit.getLogger().warning("[ajAntiXray] An error occured while trying to start bStats: " + e.getMessage());
		}
		
		this.saveDefaultConfig();
		reloadMainConfig();
		getServer().getPluginManager().registerEvents(this, this);
		
		
		Bukkit.getScheduler().runTaskTimer(this, new Runnable() {

			@Override
			public void run() {
				
				notifyAdmins();
			}
			
		}, 20, 120*20);
		
		Bukkit.getConsoleSender().sendMessage("§aajAntiXray §2v§a"+this.getDescription().getVersion()+" §2made by §aajgeiss0702 §2has been enabled!");
	}
	
	@Override
	public void onDisable() {
		Bukkit.getConsoleSender().sendMessage("§cajAntiXray §4v§c"+this.getDescription().getVersion()+" §4made by §cajgeiss0702 §4has been disabled!");
	}
	
	Map<UUID, Long> lastNotify = new HashMap<UUID, Long>();
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerBreakBlock(BlockBreakEvent e) {
		
		String block = e.getBlock().getType().toString();
		Location blockloc = e.getBlock().getLocation();
		
		if(disabledWorlds.indexOf(blockloc.getWorld().getName()) != -1) return;
		
		if(blocks.contains(block)) {
			
			if(checkFactions) {
				
				
				// Detect if the factions plugin is SavageFactions
				if(Bukkit.getPluginManager().getPlugin("Factions").getDescription().getAuthors().indexOf("ProSavage") == -1) {
					
					Faction fac = BoardColl.get().getFactionAt(PS.valueOf(blockloc));
					if(fac != null) {
						MPlayer mply = MPlayer.get(e.getPlayer());
						if(mply.getFaction().getId() == fac.getId() && fac.getId() != FactionColl.get().getNone().getId()) {
							return;
						}
					}
					
				} else {
					//The factions plugin is SavageFactions
					
					if(new SavageFactions(blockloc, e.getPlayer()).getResult()) {
						return;
					}
					
				}
				
				
				
			}
			
			Map<Long, String> player = players.get(e.getPlayer().getUniqueId());
			if(player == null) {
				player = new HashMap<Long, String>();
			}
			player.put(new Date().getTime(), block);
			players.put(e.getPlayer().getUniqueId(), player);
			
			
			UUID uuid = e.getPlayer().getUniqueId();
			if(lastNotify.containsKey(e.getPlayer().getUniqueId())) {
				//Bukkit.getLogger().info("has key");
				Long ln = lastNotify.get(uuid);
				if(new Date().getTime() - ln >= 30e3) {
					//Bukkit.getLogger().info("time good " + (new Date().getTime() - ln)/1000);
					notifyAdmins(e.getPlayer());
				}
			} else {
				//Bukkit.getLogger().info("no key");
				notifyAdmins(e.getPlayer());
			}
			
			if(e.getPlayer().hasPermission("ajaxr.debug") && blockDebug) {
				e.getPlayer().sendMessage("§a"+e.getBlock().getType().toString());
			}
		} else {
			if(e.getPlayer().hasPermission("ajaxr.debug") && blockDebug) {
				e.getPlayer().sendMessage("§c"+e.getBlock().getType().toString());
			}
		}
		
	}
	
	List<Player> recentNotifees = new ArrayList<Player>();
	

	private void notifyAdmins(Player player) {
		
		if(player == null) {
			return;
		} else if(!player.isOnline()) {
			return;
		}
		
		UUID puuid = player.getUniqueId();
		Map<String, Integer> bks = this.getBlocks(puuid);
		for(String bk : bks.keySet()) {
			if(bks.get(bk) >= warnBlocks.get(bk)) {
				if(recentNotifees.contains(player)) {
					recentNotifees.remove(player);
					//Bukkit.getLogger().info("[ajAntiXray] Skipping player " + player.getName());
					break;
				}
				lastNotify.put(puuid, new Date().getTime());
				//Bukkit.getLogger().info("[ajAntiXray] "+i+"/"+(bks.keySet().size()-2));
				Bukkit.getScheduler().runTaskLater(this, new Runnable(){
					public void run() {
						if(!recentNotifees.contains(player)) {
							recentNotifees.add(player);
						}
						Bukkit.broadcast(msgs.get("notify.format")
								.replaceAll("\\{PLAYER\\}", player.getName())
								.replaceAll("\\{COUNT\\}", bks.get(bk)+"")
								.replaceAll("\\{ORE\\}", bk)
								.replaceAll("\\{DELAY\\}", (delay/60000)+""), "ajaxr.notify");
						if(!notifySound.equalsIgnoreCase("none")) {
							for(Player p : Bukkit.getOnlinePlayers()) {
								if(p.hasPermission("ajaxr.notify")) {
									try {
										Sound sound = Sound.valueOf(notifySound);
										p.playSound(p.getLocation(), sound, 1f, 1f);
									} catch(Exception e) {
										Bukkit.getLogger().warning("[ajAntiXray] Could not find sound '"+notifySound+"'!");
										break;
									}
								}
							}
						}
						for(String command : commands) {
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("\\{PLAYER\\}", player.getName())
								.replaceAll("\\{COUNT\\}", bks.get(bk)+"")
								.replaceAll("\\{ORE\\}", bk)
								.replaceAll("\\{DELAY\\}", (delay/60000)+"")
								);
						}
					}
				}, (long) (Math.floor((Math.random()*2) * 20)));
			}
		}
	}
	
	private void notifyAdmins() {
		for(UUID puuid : players.keySet()) {
			notifyAdmins(Bukkit.getPlayer(puuid));
		}
	}
	
	
	@SuppressWarnings("unlikely-arg-type")
	public void onQuit(PlayerQuitEvent e) {
		if(players.keySet().contains(e.getPlayer())) {
			players.remove(e.getPlayer().getUniqueId());
		}
	}
	

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(label.equalsIgnoreCase("ajecho")) {
			String message = "";
			for(String arg : args) {
				message += arg+" ";
			}
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
			return true;
		}
			if(args.length == 1) {
				if(!args[0].equalsIgnoreCase("reload")) {
					if(!sender.hasPermission("ajaxr.check")) {
						sender.sendMessage(msgs.get("noperm"));
						return true;
					}
					if(Bukkit.getPlayer(args[0]) == null) {
						sender.sendMessage(msgs.get("player-not-found").replaceAll("\\{PLAYER\\}", args[0]));
						return true;
					}
					Player p = (Player) Bukkit.getPlayer(args[0]);
					
					String add = msgs.get("get.header").replaceAll("\\{PLAYER\\}", p.getName())+"\n";
					Map<String, Integer> bks = getBlocks(p.getUniqueId());
					for(String block : bks.keySet()) {
						int blocknum = bks.get(block);
						int blockmax = warnBlocks.get(block);
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
						add += msgs.get("get.format")
								.replaceAll("\\{BLOCK\\}", block)
								.replaceAll("\\{COUNTCOLOR\\}", "§"+countcolor)
								.replaceAll("\\{COUNT\\}", blocknum+"")
								.replaceAll("\\{DELAY\\}", (delay/60000)+"") + "\n";
						
					}
					sender.sendMessage(add);
					return true;
				} else if(args[0].equalsIgnoreCase("reload")) {
					if(!sender.hasPermission("ajaxr.reload")) {
						sender.sendMessage(msgs.get("noperm"));
						return true;
					}
					reloadMainConfig();
					msgs.reload();
					sender.sendMessage(msgs.get("config-reloaded"));
					return true;
				}
			}
			
			sender.sendMessage(msgs.get("cmd-syntax").replaceAll("\\{CMD\\}", label));
		
		return true;
	}
}
