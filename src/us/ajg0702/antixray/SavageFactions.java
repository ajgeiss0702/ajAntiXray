package us.ajg0702.antixray;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;

public class SavageFactions {
	
	boolean result = false;

	public SavageFactions(Location loc, Player player) {
		Faction fac = Board.getInstance().getFactionAt(new FLocation(loc));
		if(fac != null) {
			FPlayer fply = FPlayers.getInstance().getByPlayer(player);
			if(fply.getFaction().getId() == fac.getId() && !fac.getComparisonTag().equals("wilderness")) {
				result = true;
			}
		}
	}
	
	public boolean getResult() {
		return result;
	}

}
