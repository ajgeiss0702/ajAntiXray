package us.ajg0702.antixray.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import us.ajg0702.antixray.Main;

public abstract class Hook {
    private boolean enabled = false;

    private final Main plugin;
    private final String requiredPlugin;

    public Hook(Main plugin, String requiredPlugin, boolean enabled) {
        this.plugin = plugin;
        this.requiredPlugin = requiredPlugin;
        setEnabled(enabled);
    }

    public boolean hasRequiredPlugin() {
        return Bukkit.getPluginManager().isPluginEnabled(requiredPlugin);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled && hasRequiredPlugin();
    }

    /**
     * See if the plugin should check for xray
     * @param player the player that broke the block
     * @param location the location the block was
     * @return True if should check for xray, false to skip checking for xray
     */
    public abstract boolean check(Player player, Location location);
}
