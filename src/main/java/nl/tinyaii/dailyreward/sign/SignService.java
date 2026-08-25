package nl.tinyaii.dailyreward.sign;

import nl.tinyaii.dailyreward.DailyRewardPlugin;
import nl.tinyaii.dailyreward.reward.RewardConfig;
import nl.tinyaii.dailyreward.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.LocalDate;

/**
 * 签到服务：双模式判定 → 发奖 → 里程碑；补签按缺口日逐个进行。
 */
public class SignService {

    private final DailyRewardPlugin plugin;

    public SignService(DailyRewardPlugin plugin) {
        this.plugin = plugin;
    }

    /** 签到今天 */
    public boolean sign(Player p) {
        Messages msg = plugin.getMessages();
        SignManager sm = plugin.getSignManager();
        String mode = plugin.getConfig().getString("settings.mode", "date");
        SignData d = sm.getOrCreate(p.getUniqueId());

        if (d.signedToday()) {
            msg.send(p, "already");
            return false;
        }

        // online 模式：先结算本次会话时长再判定
        if ("online".equalsIgnoreCase(mode)) {
            plugin.getTimeTracker().flush(p);
            int need = plugin.getConfig().getInt("settings.online-minutes", 30);
            int have = plugin.getTimeTracker().onlineMinutesToday(p);
            if (have < need) {
                msg.send(p, "need-online-time", "{minutes}", String.valueOf(need),
                        "{current}", String.valueOf(have));
                return false;
            }
        }

        LocalDate today = LocalDate.now();
        int beforeStreak = sm.markSigned(d, today);   // 本次之前的连签
        d.streak = beforeStreak + 1;                  // 签完的连签

        // 奖励 = 基础 × 连签倍率
        RewardConfig.Reward base = plugin.getRewardConfig().baseOf(sm.cycleDay(d));
        double step = plugin.getConfig().getDouble("streak.step", 0.1);
        int cycleDays = plugin.getConfig().getInt("streak.cycle-days", 7);
        RewardConfig.Reward reward = plugin.getRewardConfig().scaled(base, d.streak, cycleDays, step);
        plugin.getRewardConfig().give(p, reward, plugin);

        boolean broken = !d.signedYesterday() && d.total > 1 && beforeStreak == 0;
        msg.send(p, "success", "{streak}", String.valueOf(d.streak), "{total}", String.valueOf(d.total));

        // 有断签缺口则提示可补
        long gaps = countRecentGaps(d);
        if (gaps > 0) {
            msg.send(p, "streak-broken", "{cards}", String.valueOf(d.cards)
                    + (plugin.getConfig().getInt("makeup.cost", 0) > 0
                        ? " &7(输入 /签到 补签 或在GUI点击红格子)" : " &7(在GUI点击红格子)"));
        }
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        for (int m : plugin.getRewardConfig().milestoneKeys()) {
            if (d.total >= m && !d.milestonesClaimed.contains(m)) {
                msg.send(p, "milestone-done", "{days}", String.valueOf(m));
            }
        }
        sm.save();
        return true;
    }

    /**
     * 补指定日期（GUI 点击红格子入口）。
     * @return true=成功（已刷新数据，调用方重开 GUI）
     */
    public boolean makeupDay(Player p, LocalDate gap) {
        Messages msg = plugin.getMessages();
        SignManager sm = plugin.getSignManager();
        SignData d = sm.getOrCreate(p.getUniqueId());

        if (!gap.isBefore(LocalDate.now())) return false;   // 只能补过去

        String result = sm.makeupDay(d, gap);
        switch (result == null ? "ok" : result) {
            case "already":
                return true;   // 已签过，静默刷新
            case "too-far":
                p.sendMessage(Messages.color("&c该日期超出可补范围（最多倒补 &e"
                        + plugin.getConfig().getInt("makeup.max-backfill-days", 3) + "&c 天）。"));
                return false;
            case "no-card": {
                int cost = plugin.getConfig().getInt("makeup.cost", 0);
                if (cost > 0 && plugin.getEcoBridge().isAvailable()) {
                    if (!plugin.getEcoBridge().has(p.getUniqueId(), cost)) {
                        msg.send(p, "cannot-afford", "{cost}", String.valueOf(cost));
                        return false;
                    }
                    plugin.getEcoBridge().withdraw(p.getUniqueId(), cost);
                    d.cards++;
                    result = sm.makeupDay(d, gap);
                    if (result == null) {
                        msg.send(p, "bought-card", "{cost}", String.valueOf(cost),
                                "{cards}", String.valueOf(d.cards));
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                        return true;
                    }
                }
                String hint = cost > 0
                        ? msg.raw("buy-hint", "{cost}", String.valueOf(cost), "{currency}", msg.currencyName()) : "";
                msg.send(p, "no-card", "{buy_hint}", hint);
                return false;
            }
            default:
                msg.send(p, "card-used", "{cards}", String.valueOf(d.cards));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                sm.save();
                return true;
        }
    }

    /** 最近 N 天内的缺口数（提示用） */
    public long countRecentGaps(SignData d) {
        int maxBack = plugin.getConfig().getInt("makeup.max-backfill-days", 3);
        long n = 0;
        for (int i = 1; i <= maxBack; i++) {
            if (!d.history.contains(LocalDate.now().minusDays(i).toString())) n++;
        }
        return n;
    }

    /** 领取全部已达成的里程碑奖励 */
    public void claimMilestones(Player p) {
        SignData d = plugin.getSignManager().getOrCreate(p.getUniqueId());
        boolean any = false;
        for (int m : plugin.getRewardConfig().milestoneKeys()) {
            if (d.total >= m && !d.milestonesClaimed.contains(m)) {
                RewardConfig.Reward r = plugin.getRewardConfig().milestone(m);
                plugin.getRewardConfig().give(p, r, plugin);
                d.milestonesClaimed.add(m);
                any = true;
                p.sendMessage(plugin.getMessages().raw("milestone-done",
                        "{days}", String.valueOf(m)).replace("达成！", "领取完成！"));
            }
        }
        if (!any) {
            plugin.getMessages().send(p, "milestone-claimed-all");
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getSignManager().save());
        }
    }

    /** online 模式达标判定 */
    public boolean onlineReady(Player p) {
        String mode = plugin.getConfig().getString("settings.mode", "date");
        if (!"online".equalsIgnoreCase(mode)) return true;
        plugin.getTimeTracker().flush(p);
        int need = plugin.getConfig().getInt("settings.online-minutes", 30);
        return plugin.getTimeTracker().onlineMinutesToday(p) >= need;
    }
}
