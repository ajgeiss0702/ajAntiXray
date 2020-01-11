package us.ajg0702.antixray;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

public class Messages {
	
	File file;
	YamlConfiguration msgs;
	
	public String get(String key) {
		String raw;
		if(msgs.isSet(key)) {
			raw = msgs.getString(key);
		} else {
			raw = "&4| &cCould not find the message '" + key + "'!";
		}
		raw = ChatColor.translateAlternateColorCodes('&', raw);
		return raw;
	}
	
	public void reload() {
		msgs = YamlConfiguration.loadConfiguration(file);
	}
	
	
	public Messages(JavaPlugin plugin) {
		file = new File(plugin.getDataFolder(), "messages.yml");
		msgs = YamlConfiguration.loadConfiguration(file);
		Map<String, String> msgDefaults = new HashMap<String, String>();
		msgDefaults.put("get.header", "&9Ores mined for {PLAYER}");
		msgDefaults.put("get.format", "&b{BLOCK}&6: {COUNTCOLOR}{COUNT} &3in last {DELAY} minutes");
		msgDefaults.put("notify.format", "&cajAntiXray&7&l> &a{PLAYER} &2has mined &a{COUNT} {ORE}s &2in the past {DELAY} minutes! They might be xraying..");
		msgDefaults.put("must-be-ingame", "&cYou must be in-game to do that!");
		msgDefaults.put("player-not-found", "&cCould not find the player {PLAYER}");
		msgDefaults.put("noperm", "&cYou do not have permission to do this!");
		msgDefaults.put("cmd-syntax", "&cUsage: &a/{CMD} <player>");
		msgDefaults.put("config-reloaded", "&aConfig and messages reloaded!");
		
		for(String key : msgDefaults.keySet()) {
			if(!msgs.isSet(key)) {
				msgs.set(key, msgDefaults.get(key));
			}
		}
		
		Map<String, String> mv = new HashMap<String, String>();
		//mv.put("before.path", "after.path");
		
		for(String key : mv.keySet()) {
			if(msgs.isSet(key)) {
				msgs.set(mv.get(key), msgs.getString(key));
				msgs.set(key, null);
			}
		}
		
		msgs.options().header("\n\nThis is the messsages file.\nYou can change any messages that are in this file\n\nIf you want to reset a message back to the default,\ndelete the entire line the message is on and restart the server.\n\t\n\t");
		try {
			msgs.save(file);
		} catch (IOException e) {
			Bukkit.getLogger().warning("[ajAntiXray] Could not save messages file!");
		}
	}
}
