package us.ajg0702.antixray;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

public class WGManager {
	
	public static WGManager INSTANCE;
	public static WGManager getInstance() {
		return INSTANCE;
	}
	public static WGManager getInstance(Main pl) {
		if(INSTANCE == null) {
			new WGManager(pl);
		}
		return INSTANCE;
	}
	
	private BooleanFlag wgflag;
	
	boolean hasPlugin;
	
	
	private WGManager(Main plugin) {
		INSTANCE = this;
		
		hasPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
		if(!hasPlugin) {
			return;
		}
		
		FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
	    try {
	    	BooleanFlag flag = new BooleanFlag("check-for-xray");
	        registry.register(flag);
	        wgflag = flag;
	    } catch (FlagConflictException e) {
	        Bukkit.getLogger().severe("[ajAntiXray] Unable to register WorldGuard flag because there is another conflicting flag!");
	    }
		
	}
	
	
	public boolean check(Location loc) {
		if(!hasPlugin) {
			return true;
		}
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(loc.getWorld()));
		// Check to make sure that "regions" is not null
		ApplicableRegionSet set = regions.getApplicableRegions(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
		
		int lastp = -100;
		ProtectedRegion sel = null;
		
		for(ProtectedRegion r : set) {
			if(r.getPriority() > lastp) {
				sel = r;
				lastp = r.getPriority();
			}
		}
		
		if(sel == null) return true;
		return sel.getFlag(wgflag);
		
	}
}
