package com.adasjusk.pvcheck;

import fr.xephi.authme.events.LoginEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class PVCheckPlugin
extends JavaPlugin
implements Listener {
    private final Set<UUID> pvClients = ConcurrentHashMap.newKeySet();
    private final Set<UUID> remindedPersistent = ConcurrentHashMap.newKeySet();
    private Mode mode;
    private int authDelay;
    private int joinDelay;
    private boolean onceEver;
    private String prefix;
    private List<String> exactChannels;
    private String chatTemplate;
    private boolean authMePresent;
    private File remindedStore;

    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadLocalConfig();
        this.remindedStore = new File(this.getDataFolder(), "reminded.yml");
        this.loadRemindedPersistent();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        if (this.getCommand("pvreminder") != null) {
            this.getCommand("pvreminder").setExecutor(this);
            this.getCommand("pvreminder").setTabCompleter(this);
        }
    this.authMePresent = Bukkit.getPluginManager().getPlugin("AuthMe") != null;
        if (this.mode == Mode.AUTO && this.authMePresent || this.mode == Mode.AFTER_AUTH) {
            if (!this.authMePresent) {
                this.getLogger().warning("Mode '" + String.valueOf((Object)this.mode) + "' requires AuthMe, but AuthMe is not present. Falling back to BEFORE_AUTH (join-based).");
            } else {
                try {
                    Class<?> hook = Class.forName("com.adasjusk.pvcheck.PVCheckPlugin$AuthMeHook");
                    Listener l = (Listener)hook.getDeclaredConstructor(PVCheckPlugin.class).newInstance(new Object[]{this});
                    Bukkit.getPluginManager().registerEvents(l, (Plugin)this);
                    this.getLogger().info("AuthMe detected. Using after-auth checks.");
                }
                catch (Throwable t) {
                    this.getLogger().warning("Failed to initialize AuthMe hook. Falling back to BEFORE_AUTH. " + t.getMessage());
                }
            }
        } else {
            this.getLogger().info("Using before-auth checks (on join).");
        }
    }

    @Override
    public void onDisable() {
        // Ensure persistent reminders are saved on shutdown
        this.saveRemindedPersistent();
    }

    private void reloadLocalConfig() {
        FileConfiguration cfg = this.getConfig();
        String modeStr = cfg.getString("mode", "auto").toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            this.mode = Mode.valueOf(modeStr);
        }
        catch (IllegalArgumentException ex) {
            this.mode = Mode.AUTO;
        }
        this.authDelay = cfg.getInt("auth-delay-ticks", 20);
    this.joinDelay = cfg.getInt("join-delay-ticks", 40);
        // New option: if true, send only once ever (persists across restarts). Overrides once-per-session.
        this.onceEver = cfg.getBoolean("once-ever", true);
        this.prefix = cfg.getString("plasmovoice-channel-prefix", "plasmovoice:");
        this.exactChannels = cfg.getStringList("channels");
        this.chatTemplate = cfg.getString("message", "&7This server uses &aPlasmo Voice&7. Install: &bmodrinth.com/plugin/plasmo-voice");
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onChannelRegister(PlayerRegisterChannelEvent e) {
        String ch = e.getChannel();
        if (ch == null) {
            return;
        }
        String lower = ch.toLowerCase(Locale.ROOT);
        if (this.exactChannels != null && !this.exactChannels.isEmpty()) {
            for (String ex : this.exactChannels) {
                if (!lower.equalsIgnoreCase(ex)) continue;
                this.pvClients.add(e.getPlayer().getUniqueId());
                return;
            }
        } else if (this.prefix != null && !this.prefix.isEmpty() && lower.startsWith(this.prefix.toLowerCase(Locale.ROOT))) {
            this.pvClients.add(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        this.pvClients.remove(id);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (this.mode == Mode.AFTER_AUTH || this.mode == Mode.AUTO && this.authMePresent) {
            return;
        }
        Player p = e.getPlayer();
        Component msg = this.prepareMessage(p);
    this.getServer().getScheduler().runTaskLater(this, () -> this.checkAndNotify(p, msg), this.joinDelay);
    }

    private void checkAndNotify(Player p, Component chatMsg) {
        if (p == null || !p.isOnline()) {
            return;
        }
        UUID uuid = p.getUniqueId();
        if (this.onceEver && this.remindedPersistent.contains(uuid)) {
            return;
        }
        if (!this.pvClients.contains(uuid)) {
            p.sendMessage(chatMsg);
            this.remindedPersistent.add(uuid);
            this.saveRemindedPersistent();
        }
    }

    private Component prepareMessage(Player p) {
        String msg = this.chatTemplate.replace("{player}", p.getName());
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }

    private static enum Mode {
        AUTO,
        AFTER_AUTH,
        BEFORE_AUTH;

    }

    public static class AuthMeHook
    implements Listener {
        private final PVCheckPlugin plugin;

        public AuthMeHook(PVCheckPlugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onAuthLogin(LoginEvent event) {
            Player p = event.getPlayer();
            Component msg = this.plugin.prepareMessage(p);
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.plugin.checkAndNotify(p, msg), this.plugin.authDelay);
        }
    }

    private void loadRemindedPersistent() {
        try {
            if (!this.getDataFolder().exists()) {
                this.getDataFolder().mkdirs();
            }
            if (!this.remindedStore.exists()) {
                YamlConfiguration yml = new YamlConfiguration();
                yml.set("reminded", null);
                yml.save(this.remindedStore);
                return;
            }
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(this.remindedStore);
            List<String> list = yml.getStringList("reminded");
            if (list != null) {
                for (String s : list) {
                    try {
                        this.remindedPersistent.add(UUID.fromString(s));
                    } catch (IllegalArgumentException ignored) {
                        // skip invalid uuid
                    }
                }
            }
        } catch (IOException ex) {
            this.getLogger().warning("Failed to load reminded.yml: " + ex.getMessage());
        }
    }

    private void saveRemindedPersistent() {
        try {
            if (!this.getDataFolder().exists()) {
                this.getDataFolder().mkdirs();
            }
            YamlConfiguration yml = new YamlConfiguration();
            List<String> out = this.remindedPersistent.stream().map(UUID::toString).toList();
            yml.set("reminded", out);
            yml.save(this.remindedStore);
        } catch (IOException ex) {
            this.getLogger().warning("Failed to save reminded.yml: " + ex.getMessage());
        }
    }

    // --- Command handling ---
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("pvreminder")) return false;
        if (!sender.hasPermission("pvreminder.manage")) {
            sender.sendMessage("You don't have permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " <forget|remind> <player|uuid>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String targetArg = args[1];
        UUID targetUuid = parseUuidOrResolve(targetArg);
        if (targetUuid == null) {
            sender.sendMessage("Could not resolve target '" + targetArg + "'. Use online player name or UUID.");
            return true;
        }
        switch (sub) {
            case "forget" -> {
                boolean removed = this.remindedPersistent.remove(targetUuid);
                this.saveRemindedPersistent();
                sender.sendMessage(removed ? "Removed from reminded list: " + targetUuid : "Not in reminded list: " + targetUuid);
            }
            case "remind" -> {
                // Remove first as requested, then attempt to send
                this.remindedPersistent.remove(targetUuid);
                this.saveRemindedPersistent();
                Player online = Bukkit.getPlayer(targetUuid);
                if (online != null && online.isOnline()) {
                    Component msg = this.prepareMessage(online);
                    this.getServer().getScheduler().runTaskLater(this, () -> online.sendMessage(msg), 1L);
                    sender.sendMessage("Reminder sent to: " + online.getName() + " (" + targetUuid + ") and removed from list.");
                } else {
                    sender.sendMessage("Player is offline. Removed from list, but cannot send reminder now.");
                }
            }
            default -> sender.sendMessage("Unknown subcommand. Use: forget | remind");
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("pvreminder")) return java.util.Collections.emptyList();
        if (args.length == 1) {
            return java.util.Arrays.asList("forget", "remind");
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
        }
        return java.util.Collections.emptyList();
    }

    private UUID parseUuidOrResolve(String arg) {
        try {
            return UUID.fromString(arg);
        } catch (IllegalArgumentException ignored) {
        }
        Player online = Bukkit.getPlayerExact(arg);
        if (online != null) {
            return online.getUniqueId();
        }
        try {
            org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(arg);
            if (off != null && (off.hasPlayedBefore() || off.isOnline()) && off.getUniqueId() != null) {
                return off.getUniqueId();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
