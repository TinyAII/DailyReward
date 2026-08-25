package nl.tinyaii.dailyreward;

import nl.tinyaii.dailyreward.command.SignCommand;
import nl.tinyaii.dailyreward.economy.EcoBridge;
import nl.tinyaii.dailyreward.gui.MenuListener;
import nl.tinyaii.dailyreward.sign.OnlineTimeTracker;
import nl.tinyaii.dailyreward.sign.SignManager;
import nl.tinyaii.dailyreward.reward.RewardConfig;
import nl.tinyaii.dailyreward.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class DailyRewardPlugin extends JavaPlugin {

    private SignManager signManager;
    private RewardConfig rewardConfig;
    private EcoBridge ecoBridge;
    private OnlineTimeTracker timeTracker;
    private Messages messages;
    private nl.tinyaii.dailyreward.sign.SignService signService;

    @Override
    public void onEnable() {
        // TinyAII 品牌横幅 —— 必须在所有初始化逻辑之前输出（与 AutoBackup 完全一致）
        getLogger().info(" _____ _                _    ___ ___");
        getLogger().info("|_   _(_)_ __  _   _   / \\  |_ _|_ _|");
        getLogger().info("  | | | | '_ \\| | | | / _ \\  | | | |");
        getLogger().info("  | | | | | | | |_| |/ ___ \\ | | | |");
        getLogger().info("  |_| |_|_| |_|\\__, /_/   \\_\\___|___|");
        getLogger().info("               |___/");
        getLogger().info("DailyReward 每日签到 v" + getDescription().getVersion() + " - TinyAII 出品");

        saveDefaultConfig();
        messages = new Messages(this);
        rewardConfig = new RewardConfig(this);
        rewardConfig.load();
        ecoBridge = new EcoBridge(this);
        signManager = new SignManager(this);
        signManager.load();
        timeTracker = new OnlineTimeTracker(this);
        Bukkit.getPluginManager().registerEvents(timeTracker, this);
        signService = new nl.tinyaii.dailyreward.sign.SignService(this);

        getCommand("签到").setExecutor(new SignCommand(this));
        getCommand("签到").setTabCompleter(new SignCommand(this));
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        getLogger().info("每日签到已启用（模式: " + modeName() + "），共加载 "
                + signManager.size() + " 名玩家数据。指令: /签到");
    }

    @Override
    public void onDisable() {
        if (signManager != null) {
            signManager.save();
            getLogger().info("签到数据已保存，插件已卸载。");
        }
    }

    public void reloadAll() {
        reloadConfig();
        messages.reload();
        rewardConfig.load();
    }

    public String modeName() {
        return "online".equalsIgnoreCase(getConfig().getString("settings.mode", "date")) ? "在线时长" : "自然日";
    }

    public SignManager getSignManager() { return signManager; }
    public RewardConfig getRewardConfig() { return rewardConfig; }
    public EcoBridge getEcoBridge() { return ecoBridge; }
    public OnlineTimeTracker getTimeTracker() { return timeTracker; }
    public Messages getMessages() { return messages; }
    public nl.tinyaii.dailyreward.sign.SignService getSignService() { return signService; }

    public static String colorWrap(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
