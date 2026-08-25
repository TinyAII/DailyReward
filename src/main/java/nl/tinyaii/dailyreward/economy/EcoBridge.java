package nl.tinyaii.dailyreward.economy;

import nl.tinyaii.dailyreward.DailyRewardPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Economy 反射联动（Home 同款模式）：装了发金币，没装跳过。编译期零依赖。
 */
public class EcoBridge {

    private final DailyRewardPlugin plugin;
    private boolean available;
    private Method mGetBalance, mDeposit, mHas, mWithdraw;
    private String currencyName = "金币";

    public EcoBridge(DailyRewardPlugin plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().getPlugin("Economy") == null) {
            available = false;
            return;
        }
        try {
            Class<?> api = Class.forName("nl.tinyaii.economy.api.EconomyAPI");
            mGetBalance = api.getMethod("getBalance", java.util.UUID.class);
            mDeposit = api.getMethod("deposit", java.util.UUID.class, double.class);
            mHas = api.getMethod("has", java.util.UUID.class, double.class);
            mWithdraw = api.getMethod("withdraw", java.util.UUID.class, double.class);
            try {
                Plugin eco = Bukkit.getPluginManager().getPlugin("Economy");
                File cfgFile = new File(eco.getDataFolder(), "config.yml");
                if (cfgFile.exists()) {
                    org.bukkit.configuration.file.YamlConfiguration yml =
                            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(cfgFile);
                    currencyName = yml.getString("settings.currency-name", "金币");
                }
            } catch (Throwable ignored) {}
            available = true;
            plugin.getLogger().info("已检测到 Economy 插件，签到金币奖励启用。");
        } catch (Throwable t) {
            plugin.getLogger().warning("Economy API 反射失败（金币奖励禁用）: " + t.getMessage());
            available = false;
        }
    }

    public boolean isAvailable() { return available; }

    public void deposit(java.util.UUID uuid, double amount) {
        if (!available || amount <= 0) return;
        try { mDeposit.invoke(null, uuid, amount); } catch (Exception ignored) {}
    }

    public boolean has(java.util.UUID uuid, double amount) {
        if (!available) return true;
        try { return (Boolean) mHas.invoke(null, uuid, amount); }
        catch (Exception e) { return true; }
    }

    public boolean withdraw(java.util.UUID uuid, double amount) {
        if (!available || amount <= 0) return true;
        try { return (Boolean) mWithdraw.invoke(null, uuid, amount); }
        catch (Exception e) { return false; }
    }

    public String getCurrencyName() { return currencyName; }
}
