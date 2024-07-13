package us.ajg0702.antixray.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import us.ajg0702.antixray.Main;

public class WorldGuard extends Hook {
	
	private BooleanFlag wgFlag;

	public WorldGuard(Main plugin, boolean enabled) {
		super(plugin, "WorldGuard", enabled);
		if(hasRequiredPlugin()) {
			plugin.getLogger().info("Registering worldguard flag");
			FlagRegistry registry = com.sk89q.worldguard.WorldGuard.getInstance().getFlagRegistry();
			try {
				BooleanFlag flag = new BooleanFlag("check-for-xray");
				registry.register(flag);
				wgFlag = flag;
			} catch (FlagConflictException e) {
				plugin.getLogger().severe("Unable to register WorldGuard flag because there is another conflicting flag!");
			}
		}
	}



	@Override
	public boolean check(Player player, Location location) {
		if(wgFlag == null) return true;
		RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));
		// Check to make sure that "regions" is not null
		assert regions != null;
		ApplicableRegionSet set = regions.getApplicableRegions(BlockVector3.at(location.getX(), location.getY(), location.getZ()));

		int lastp = -100;
		ProtectedRegion sel = null;

		for(ProtectedRegion r : set) {
			if(r.getPriority() > lastp) {
				sel = r;
				lastp = r.getPriority();
			}
		}

		if(sel == null) return true;
		return !Boolean.FALSE.equals(sel.getFlag(wgFlag));//*/return true;
	}
}
