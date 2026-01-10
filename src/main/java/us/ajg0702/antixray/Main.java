package us.ajg0702.antixray;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.ConfigurateException;
import us.ajg0702.antixray.hooks.Hook;
import us.ajg0702.antixray.hooks.HookRegistry;
import us.ajg0702.antixray.hooks.WorldGuard;
import us.ajg0702.utils.common.Config;
import us.ajg0702.utils.common.Messages;

import java.util.*;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap; // CHANGED (Folia): use thread-safe collections
import java.util.logging.Level;

public class Main extends JavaPlugin {

    private HookRegistry hookRegistry;

    // CHANGED (Folia): ConcurrentHashMap because this map is accessed from multiple schedulers/threads
    // (event thread, global region scheduler, async scheduler).
    final Map<UUID, Map<Long, String>> players = new ConcurrentHashMap<>();

    List<String> blocks;
    Map<String, Integer> warnBlocks = new HashMap<>();
    int delay;

    List<String> disabledWorlds;

    Messages messages;

    Config config;

    int ignoreAbove = 64;

    // CHANGED (Folia): keep this as a method-local HashMap (safe), but the per-player map stored in
    // `players` should also be concurrent / safely mutated.
    Map<String, Integer> getBlocks(UUID uuid) {
        Map<String, Integer> bks = new HashMap<>();
        for (String block : blocks) {
            bks.put(block, 0);
        }

        // CHANGED (Folia): ensure the player map exists and is thread-safe.
        Map<Long, String> player = players.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        // CHANGED (Folia): iterating + removing while other threads might write is safe on CHM via iterator.remove?
        // CHM's iterators do NOT support remove(). So we do a two-pass approach.
        long cutoff = System.currentTimeMillis() - delay;
        List<Long> toRemove = new ArrayList<>();

        for (Map.Entry<Long, String> entry : player.entrySet()) {
            long t = entry.getKey();
            if (t < cutoff) {
                toRemove.add(t);
                continue;
            }
            String bk = entry.getValue();
            Integer before = bks.get(bk);
            if (before == null) continue;
            bks.put(bk, before + 1);
        }

        // CHANGED (Folia): remove old entries after iteration
        for (Long t : toRemove) {
            player.remove(t);
        }

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
        for (String block : blocksTemp) {
            String[] parts = block.split(":");
            if (parts.length > 1 && (parts[0] != null || parts[1] != null)) {
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

        if (wgHook != null) {
            wgHook.setEnabled(config.getBoolean("worldguard-integration"));
        }
        if (wgHook != null && wgHook.isEnabled()) {
            getLogger().info("Enabled WorldGuard hook and flag!");
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
            if (config.getBoolean("worldguard-hook")) {
                hookRegistry.add(new WorldGuard(this, false));
            }
        } catch (NoClassDefFoundError ignored) {
        }

    }

    @Override
    public void onEnable() {

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
        msgDefaults.put("get.header", "&9Ores mined for &b{PLAYER}&9:");
        msgDefaults.put("get.format", "&b{BLOCK}&6: {COUNTCOLOR}{COUNT} &3in last &b{DELAY}&3 minutes");
        msgDefaults.put("notify.format", "<hover:show_text:'<green>Click to teleport to {PLAYER}'><click:run_command:/tp {PLAYER}>&cajAntiXray&7<bold>></bold> &a{PLAYER} &2has mined &a{COUNT} {ORE}s &2in the past {DELAY} minutes! They might be xraying..</click></hover>");
        msgDefaults.put("webhook.format", "**{PLAYER}** has mined **{COUNT} {ORE}s** in the past {DELAY} minutes! They might be xraying..");
        msgDefaults.put("must-be-ingame", "&cYou must be in-game to do that!");
        msgDefaults.put("player-not-found", "&cCould not find the player {PLAYER}");
        msgDefaults.put("noperm", "&cYou do not have permission to do this!");
        msgDefaults.put("cmd-syntax", "&cUsage: &a/{CMD} <player>");
        msgDefaults.put("config-reloaded", "&aConfig and messages reloaded!");

        messages = new Messages(getDataFolder(), getLogger(), msgDefaults);

        reloadMainConfig();

        // CHANGED (Folia): global region scheduler is correct for repeating global task
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> notifyAdmins(), 20L, 120L * 20L);

        Bukkit.getConsoleSender().sendMessage("§aajAntiXray §2v§a" + this.getDescription().getVersion() + " §2made by §aajgeiss0702 §2has been enabled!");
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
        Bukkit.getConsoleSender().sendMessage("§cajAntiXray §4v§c" + this.getDescription().getVersion() + " §4made by §cajgeiss0702 §4has been disabled!");
    }

    // CHANGED (Folia): thread-safe map
    final Map<UUID, Long> lastNotify = new ConcurrentHashMap<>();

    // CHANGED (Folia): do NOT store Player objects across schedulers; store UUIDs instead
    final Set<UUID> recentNotifees = java.util.concurrent.ConcurrentHashMap.newKeySet();


    void notifyAdmins(Player player) {

        if (player == null) {
            return;
        } else if (!player.isOnline()) {
            return;
        }

        final UUID puuid = player.getUniqueId();
        final Map<String, Integer> bks = this.getBlocks(puuid);

        for (Map.Entry<String, Integer> entry : bks.entrySet()) {
            final String ore = entry.getKey();
            final Integer count = entry.getValue();
            final Integer max = warnBlocks.get(ore);

            if (count == null || max == null) continue;

            if (count >= max) {

                // Folia: use UUID set instead of Player list
                if (recentNotifees.contains(puuid)) {
                    recentNotifees.remove(puuid);
                    break;
                }

                lastNotify.put(puuid, System.currentTimeMillis());

                // snapshot everything used later
                final NotifyCtx ctx = new NotifyCtx(
                        puuid,
                        player.getName(),
                        count,
                        ore,
                        delay / 60000
                );

                // Folia: resolve sound ONCE (not per-player) + validate
                final Sound notifyBukkitSound;
                if (notifySound != null && !notifySound.equalsIgnoreCase("none")) {
                    NamespacedKey key = NamespacedKey.minecraft(notifySound.toLowerCase(Locale.ROOT));
                    notifyBukkitSound = Bukkit.getRegistry(Sound.class).get(key);
                    if (notifyBukkitSound == null) {
                        Bukkit.getLogger().warning("[ajAntiXray] Invalid notify-sound: " + notifySound);
                    }
                } else {
                    notifyBukkitSound = null;
                }

                scheduleNotifyAndActions(ctx, notifyBukkitSound);
            }
        }
    }

    private void notifyAdmins() {
        // CHANGED (Folia): iterate over snapshot of keys to avoid concurrent modification surprises
        for (UUID puuid : new ArrayList<>(players.keySet())) {
            Player p = Bukkit.getPlayer(puuid);
            if (p != null) {
                notifyAdmins(p);
            }
        }
    }


    private record NotifyCtx(UUID puuid, String playerName, int minedCount, String ore, int delayMinutes) {}

    private void scheduleNotifyAndActions(NotifyCtx ctx, Sound notifyBukkitSound){
        long delayTicks = java.util.concurrent.ThreadLocalRandom.current().nextLong(1, 41);

        Bukkit.getGlobalRegionScheduler().runDelayed(this, task -> {
            recentNotifees.add(ctx.puuid());

            notifyOnlineAdmins(ctx, notifyBukkitSound);
            runConsoleCommands(ctx);
            sendWebhookAsync(ctx);
        }, delayTicks);
    }

    private String[] msgArgs(NotifyCtx c) {
        return new String[] {
                "PLAYER:" + c.playerName(),
                "COUNT:" + c.minedCount(),
                "ORE:" + c.ore(),
                "DELAY:" + c.delayMinutes()
        };
    }

    /** Feed in player data and notify anyone who has the admin permissions and play a sound provided by the config*/
    private void notifyOnlineAdmins(NotifyCtx ctx, Sound notifyBukkitSound) {
        final String[] args = msgArgs(ctx);

        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (!admin.hasPermission("ajaxr.notify")) continue;

            admin.getScheduler().run(this, adminTask -> {
                admin.sendMessage(messages.getComponent("notify.format", args));

                if (notifyBukkitSound != null) {
                    admin.playSound(admin.getLocation(), notifyBukkitSound, 1f, 1f);
                }
            }, null);
        }
    }

    private void runConsoleCommands(NotifyCtx ctx) {
        for (String command : commands) {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command.replace("{PLAYER}", ctx.playerName())
                            .replace("{COUNT}", String.valueOf(ctx.minedCount()))
                            .replace("{ORE}", ctx.ore())
                            .replace("{DELAY}", String.valueOf(ctx.delayMinutes()))
            );
        }
    }

    private void sendWebhookAsync(NotifyCtx ctx) {
        final String webhookUrl = config.getString("discord-webhook");
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        final String webhookMessage = messages.getString("webhook.format", msgArgs(ctx));

        Bukkit.getAsyncScheduler().runNow(this, asyncTask ->
                WebhookSender.send(getLogger(), webhookUrl, webhookMessage)
        );
    }

    /*
    private void scheduleNotifyAndActions(
            UUID puuid,
            String playerName,
            int minedCount,
            String ore,
            int delayMinutes,
            Sound notifyBukkitSound
    ) {
        //Calculate delay in tickets, never let it be 0
        long delayTicks = java.util.concurrent.ThreadLocalRandom.current().nextLong(1,41);

        Bukkit.getGlobalRegionScheduler().runDelayed().runDelayed(this, task -> {
            // Track "recently notified"
            recentNotifees.add(puuid);

            // Notify admins (player safe via per-admin scheduler)
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (!admin.hasPermission("ajaxr.notify")) continue;

                admin.getScheduler().run(this adminTask -> {
                    admin.sendMessage(
                            messages.getComponent(
                                    "notify.format",
                                    "PLAYER:" + playerName,
                                    "COUNT:" + minedCount,
                                    "ORE:" + ore,
                                    "DELAY:" + delayMinutes
                            )
                    );

                    if (notifyBukkitSound != null) {
                        admin.playSound(admin.getLocation(), notifyBukkitSound, 1f, 1f);
                    }
                }, null);
            }

            // Console commands (safe on global)
            for (String command : commands) {
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        command.replace("{PLAYER}", playerName)
                        .replace("{COUNT}", String.valueOf(minedCount))
                        .replace("{ORE}", ore)
                        .replace("{DELAY}", String.valueOf(delayMinutes))
                );
            }

            // Webhook (must be async)
            final String webhookUrl = config.getString("discord-webhook");
            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                final String webhookMessage = messages.getString(
                        "webhook.format",
                        "PLAYER:" + playerName,
                        "COUNT:" + minedCount,
                        "ORE:" + ore,
                        "DELAY:" + delayMinutes
                );

                Bukkit.getAsyncScheduler().runNow(this, asyncTask -> WebhookSender.send(getLogger(), webhookUrl, webhookMessage));
            }
        }, delayTicks);
    }


    */
}
