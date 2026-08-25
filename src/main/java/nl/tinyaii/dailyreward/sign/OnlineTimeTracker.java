package nl.tinyaii.dailyreward.sign;

import nl.tinyaii.dailyreward.DailyRewardPlugin;
import org.bukkit.entity.Player;

/**
 * 在线时长追踪：join 开始计时，quit 累加；online 模式判定用。
 */
public class OnlineTimeTracker implements org.bukkit.event.Listener {

    private final DailyRewardPlugin plugin;
    private final java.util.Map<java.util.UUID, Long> joinTime = new java.util.concurrent.ConcurrentHashMap<>();

    public OnlineTimeTracker(DailyRewardPlugin plugin) {
        this.plugin = plugin;
        // 服务器已运行时补录当前在线玩家
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            joinTime.put(p.getUniqueId(), System.currentTimeMillis());
            // 从 data 里恢复今日已累计秒数（SignManager.load 已做）
        }
    }

    @org.bukkit.event.EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
        joinTime.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @org.bukkit.event.EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        accumulate(e.getPlayer());
        joinTime.remove(e.getPlayer().getUniqueId());
    }

    /** 把本次在线时间累加到 SignData（签到判定前调用） */
    public void flush(Player p) {
        accumulate(p);
    }

    private void accumulate(Player p) {
        Long start = joinTime.get(p.getUniqueId());
        if (start == null) return;
        long sessionSeconds = (System.currentTimeMillis() - start) / 1000;
        joinTime.put(p.getUniqueId(), System.currentTimeMillis()); // 重置起点
        if (sessionSeconds <= 0) return;

        String mode = plugin.getConfig().getString("settings.mode", "date");
        // date 模式不记录在线时长（省内存）
        if (!"online".equalsIgnoreCase(mode)) return;

        SignData d = plugin.getSignManager().getOrCreate(p.getUniqueId());
        String today = java.time.LocalDate.now().toString();
        if (!today.equals(d.onlineDate)) {   // 跨日清零
            d.onlineDate = today;
            d.onlineSecondsToday = 0;
        }
        d.onlineSecondsToday += sessionSeconds;
        plugin.getSignManager().save();
    }

    /** 当日累计在线分钟数 */
    public int onlineMinutesToday(Player p) {
        SignData d = plugin.getSignManager().getOrCreate(p.getUniqueId());
        String today = java.time.LocalDate.now().toString();
        long secs = today.equals(d.onlineDate) ? d.onlineSecondsToday : 0;
        Long start = joinTime.get(p.getUniqueId());
        if (start != null && "online".equalsIgnoreCase(plugin.getConfig().getString("settings.mode", "date"))) {
            secs += (System.currentTimeMillis() - start) / 1000;
        }
        return (int) (secs / 60);
    }
}
