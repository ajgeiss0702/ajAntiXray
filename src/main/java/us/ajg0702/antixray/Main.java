package us.ajg0702.antixray;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.ConfigurateException;
import us.ajg0702.antixray.hooks.Hook;
import us.ajg0702.antixray.hooks.HookRegistry;
import us.ajg0702.antixray.hooks.SavageFactions;
import us.ajg0702.antixray.hooks.WorldGuard;
import us.ajg0702.utils.common.Config;
import us.ajg0702.utils.common.Messages;

import java.util.*;
import java.util.logging.Level;

public class Main extends JavaPlugin {

	private HookRegistry hookRegistry;
	
	Map<UUID, Map<Long, String>> players = new HashMap<>();
	
	List<String> blocks;
	Map<String, Integer> warnBlocks = new HashMap<>();
	int delay;
	
	List<String> disabledWorlds;
	
	Messages messages;

	Config config;
	
	int ignoreAbove = 64;

	private BukkitAudiences adventure;
	
	Map<String, Integer> getBlocks(UUID uuid) {
		Map<String, Integer> bks = new HashMap<>();
		for(String block : blocks) {
			bks.put(block, 0);
		}
		Map<Long, String> player = players.get(uuid);
		if(player == null) {
			player = new HashMap<>();
		}
		Iterator<Long> i = player.keySet().iterator();
		while(i.hasNext()) {
			 long t = i.next();
			if(t < System.currentTimeMillis()-delay) {
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
	
	List<String> commands;
	
	String notifySound = "NONE";
	
	
	void reloadMainConfig() {
		try {
			config.reload();
		} catch (ConfigurateException e) {
			getLogger().log(Level.WARNING, "Unable to reload config: ", e);
			return;
		}
		List<String> blocksTemp = config.getStringList("blocks");
		blocks = new ArrayList<>();
		warnBlocks = new HashMap<>();
		for(String block : blocksTemp) {
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
		delay = config.getInt("blocks-in-last-minutes") * 60000;


		Hook wgHook = getHookRegistry().getHook(WorldGuard.class);
		Hook sfHook = getHookRegistry().getHook(SavageFactions.class);

		if(wgHook != null) {
			wgHook.setEnabled(config.getBoolean("worldguard-integration"));
		}
		sfHook.setEnabled(config.getBoolean("factions-integration"));

		if(wgHook != null && wgHook.isEnabled()) {
			getLogger().info("Enabled WorldGuard hook and flag!");
		}
		if(sfHook.isEnabled()) {
			getLogger().info("Enabled SavageFactions hook and flag!");
		}

		disabledWorlds = config.getStringList("disabled-worlds");

		blockDebug = config.getBoolean("block-debug");

		commands = config.getStringList("commands-to-execute");

		ignoreAbove = config.getInt("ignore-above-y");

		notifySound = config.getString("notify-sound");

	}
	
	Metrics stats;

	@Override
	public void onLoad() {

		try {
			config = new Config(getDataFolder(), getLogger());
		} catch (ConfigurateException e) {
			getLogger().log(Level.SEVERE, "Failed to load config", e);
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		hookRegistry = new HookRegistry();

		try {
			if(config.getBoolean("worldguard-hook")) {
				hookRegistry.add(new WorldGuard(this, false));
			}
		} catch(NoClassDefFoundError ignored) {}
		hookRegistry.add(new SavageFactions(this, false));
	}

	@Override
	public void onEnable() {

		this.adventure = BukkitAudiences.create(this);
		
		try {
			stats = new Metrics(this);
		} catch (Exception e) {
			Bukkit.getLogger().warning("[ajAntiXray] An error occured while trying to start bStats: " + e.getMessage());
		}
		
		
		Commands commands = new Commands(this);

		getServer().getPluginManager().registerEvents(new Listener(this), this);
		getCommand("ajantixray").setExecutor(commands);
		getCommand("ajecho").setExecutor(commands);

		LinkedHashMap<String, Object> msgDefaults = new LinkedHashMap<>();
		msgDefaults.put("get.header", "&9Ores mined for {PLAYER}");
		msgDefaults.put("get.format", "&b{BLOCK}&6: {COUNTCOLOR}{COUNT} &3in last {DELAY} minutes");
		msgDefaults.put("notify.format", "<hover:show_text:'<green>Click to teleport to {PLAYER}'><click:run_command:/tp {PLAYER}>&cajAntiXray&7<bold>></bold> &a{PLAYER} &2has mined &a{COUNT} {ORE}s &2in the past {DELAY} minutes! They might be xraying..</click></hover>");
		msgDefaults.put("webhook.format", "**{PLAYER}** has mined **{COUNT} {ORE}s** in the past {DELAY} minutes! They might be xraying..");
		msgDefaults.put("must-be-ingame", "&cYou must be in-game to do that!");
		msgDefaults.put("player-not-found", "&cCould not find the player {PLAYER}");
		msgDefaults.put("noperm", "&cYou do not have permission to do this!");
		msgDefaults.put("cmd-syntax", "&cUsage: &a/{CMD} <player>");
		msgDefaults.put("config-reloaded", "&aConfig and messages reloaded!");

		messages = new Messages(getDataFolder(), getLogger(), msgDefaults);

		reloadMainConfig();

		
		Bukkit.getScheduler().runTaskTimer(this, this::notifyAdmins, 20, 120*20);
		
		Bukkit.getConsoleSender().sendMessage("§aajAntiXray §2v§a"+this.getDescription().getVersion()+" §2made by §aajgeiss0702 §2has been enabled!");
	}

	public Config getAConfig() {
		return config;
	}

	public Messages getMessages() {
		return messages;
	}

	public HookRegistry getHookRegistry() {
		return hookRegistry;
	}

	@Override
	public void onDisable() {
		if(this.adventure != null) {
			this.adventure.close();
			this.adventure = null;
		}
		Bukkit.getConsoleSender().sendMessage("§cajAntiXray §4v§c"+this.getDescription().getVersion()+" §4made by §cajgeiss0702 §4has been disabled!");
	}
	
	Map<UUID, Long> lastNotify = new HashMap<UUID, Long>();
	
	List<Player> recentNotifees = new ArrayList<Player>();
	

	void notifyAdmins(Player player) {
		
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
				lastNotify.put(puuid, System.currentTimeMillis());
				//Bukkit.getLogger().info("[ajAntiXray] "+i+"/"+(bks.keySet().size()-2));
				Bukkit.getScheduler().runTaskLater(this, new Runnable(){
					public void run() {
						if(!recentNotifees.contains(player)) {
							recentNotifees.add(player);
						}
						for(Player admin : Bukkit.getOnlinePlayers()) {
							if(!admin.hasPermission("ajaxr.notify")) continue;
							adventure.player(admin).sendMessage(messages.getComponent(
									"notify.format",
									"PLAYER:" + player.getName(),
									"COUNT:" + bks.get(bk),
									"ORE:" + bk,
									"DELAY:" + (delay/60000)
							));
						}
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
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("\\{PLAYER}", player.getName())
								.replaceAll("\\{COUNT}", bks.get(bk)+"")
								.replaceAll("\\{ORE}", bk)
								.replaceAll("\\{DELAY}", (delay/60000)+"")
								);
						}

						String webhookUrl = config.getString("discord-webhook");
						if(!webhookUrl.isEmpty()) {
							String webhookMessage = messages.getString(
									"webhook.format",
									"PLAYER:" + player.getName(),
									"COUNT:" + bks.get(bk),
									"ORE:" + bk,
									"DELAY:" + (delay/60000)
							);
							WebhookSender.send(getLogger(), webhookUrl, webhookMessage);
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


	public BukkitAudiences adventure() {
		if(this.adventure == null) {
			throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
		}
		return this.adventure;
	}
}
