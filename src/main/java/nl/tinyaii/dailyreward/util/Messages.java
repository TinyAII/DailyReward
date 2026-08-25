package nl.tinyaii.dailyreward.util;

import nl.tinyaii.dailyreward.DailyRewardPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

/**
 * 消息工具：统一前缀 + 占位符替换。
 */
public class Messages {
    private final DailyRewardPlugin plugin;
    private YamlConfiguration cfg;
    private String prefix;

    public Messages(DailyRewardPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File f = new File(plugin.getDataFolder(), "config.yml");
        if (!f.exists()) plugin.saveDefaultConfig();
        cfg = YamlConfiguration.loadConfiguration(f);
        prefix = color(cfg.getString("messages.prefix", ""));
    }

    public String raw(String key, String... repl) {
        String msg = cfg.getString("messages." + key, key);
        for (int i = 0; i + 1 < repl.length; i += 2) {
            msg = msg.replace(repl[i], repl[i + 1]);
        }
        return prefix + color(msg);
    }

    public void send(Player p, String key, String... repl) {
        if (p != null) p.sendMessage(raw(key, repl));
    }

    /** 无前缀原文（GUI/Title 用） */
    public String bare(String key, String... repl) {
        String msg = cfg.getString("messages." + key, key);
        for (int i = 0; i + 1 < repl.length; i += 2) {
            msg = msg.replace(repl[i], repl[i + 1]);
        }
        return color(msg);
    }

    public String currencyName() {
        return plugin.getEcoBridge().getCurrencyName();
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
