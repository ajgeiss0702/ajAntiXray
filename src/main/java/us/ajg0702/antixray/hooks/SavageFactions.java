package us.ajg0702.antixray.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import org.bukkit.plugin.Plugin;
import us.ajg0702.antixray.Main;

import java.util.Objects;

public class SavageFactions extends Hook {


	public SavageFactions(Main plugin, boolean enabled) {
		super(plugin, "Factions", enabled);
	}

	@Override
	public boolean hasRequiredPlugin() {
		if(!super.hasRequiredPlugin()) return false;
		Plugin plugin = Bukkit.getPluginManager().getPlugin("Factions");
		if(plugin == null) return false;
		return plugin.getDescription().getAuthors().contains("ProSavage");
	}

	@Override
	public boolean check(Player player, Location location) {
		Faction fac = Board.getInstance().getFactionAt(new FLocation(location));
		if(fac != null) {
			FPlayer fply = FPlayers.getInstance().getByPlayer(player);
			if(Objects.equals(fply.getFaction().getId(), fac.getId()) && !fac.getComparisonTag().equals("wilderness")) {
				return false;
			}
		}
		return true;
	}
}
