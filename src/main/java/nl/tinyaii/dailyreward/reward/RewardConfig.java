package nl.tinyaii.dailyreward.reward;

import nl.tinyaii.dailyreward.DailyRewardPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 奖励表解析与发放（每日/里程碑共用结构：money + cards + items + commands）。
 */
public class RewardConfig {

    public static class Reward {
        public double money = 0;
        public int cards = 0;
        public final List<ItemStack> items = new ArrayList<>();
        public final List<String> commands = new ArrayList<>(); // {player} 占位
    }

    private final DailyRewardPlugin plugin;
    /** cycle-day(1~7) → 基础奖励 */
    private final Map<Integer, Reward> daily = new LinkedHashMap<>();
    /** 里程碑天数 → 奖励 */
    private final Map<Integer, Reward> milestones = new LinkedHashMap<>();

    public RewardConfig(DailyRewardPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        daily.clear();
        milestones.clear();
        ConfigurationSection rs = plugin.getConfig().getConfigurationSection("rewards");
        if (rs != null) {
            for (String key : rs.getKeys(false)) {
                try {
                    int day = Integer.parseInt(key.replace("day-", ""));
                    Reward r = parse(rs.getConfigurationSection(key));
                    if (r != null) daily.put(day, r);
                } catch (NumberFormatException ignored) {}
            }
        }
        ConfigurationSection ms = plugin.getConfig().getConfigurationSection("milestone-rewards");
        if (ms != null) {
            for (String key : ms.getKeys(false)) {
                try {
                    milestones.put(Integer.parseInt(key), parse(ms.getConfigurationSection(key)));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    private Reward parse(ConfigurationSection s) {
        if (s == null) return null;
        Reward r = new Reward();
        r.money = s.getDouble("money", 0);
        r.cards = s.getInt("cards", 0);
        for (Map<?, ?> m : s.getMapList("items")) {
            Object mat = m.get("material");
            if (mat == null) continue;
            Material material = Material.matchMaterial(mat.toString());
            if (material == null) continue;
            int amount = 1;
            Object a = m.get("amount");
            if (a instanceof Number) amount = ((Number) a).intValue();
            r.items.add(new ItemStack(material, Math.max(1, amount)));
        }
        r.commands.addAll(s.getStringList("commands"));
        return r;
    }

    /** 第 cycleDay 天的基础奖励（1~7 循环） */
    public Reward baseOf(int cycleDay) {
        return daily.getOrDefault(((cycleDay - 1) % 7) + 1, daily.get(1));
    }

    /** 实发奖励 = 基础 × 连签倍率（金币取整，物品数量按倍率放大后至少1、向上取整） */
    public Reward scaled(Reward base, int streak, int cycleDays, double step) {
        double mult = 1.0 + (streak - 1) * step;
        boolean jackpot = ((streak - 1) % Math.max(1, cycleDays)) == (cycleDays - 1);
        if (jackpot) mult *= 2.0;

        Reward out = new Reward();
        out.money = Math.round(base.money * mult);
        out.cards = base.cards;
        for (ItemStack it : base.items) {
            ItemStack copy = it.clone();
            copy.setAmount(Math.max(1, (int) Math.ceil(it.getAmount() * mult)));
            out.items.add(copy);
        }
        out.commands.addAll(base.commands);
        return out;
    }

    public Reward milestone(int days) {
        return milestones.get(days);
    }

    public List<Integer> milestoneKeys() {
        return new ArrayList<>(milestones.keySet());
    }

    /** 发放（金币走 EcoBridge；命令控制台执行） */
    public void give(Player p, Reward r, DailyRewardPlugin pluginInstance) {
        if (r == null) return;
        nl.tinyaii.dailyreward.economy.EcoBridge eco = plugin.getEcoBridge();
        if (r.money > 0 && eco.isAvailable()) eco.deposit(p.getUniqueId(), r.money);
        for (ItemStack it : r.items) {
            Map<Integer, ItemStack> overflow = p.getInventory().addItem(it);
            for (ItemStack rest : overflow.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), rest);
            }
        }
        for (String cmd : r.commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", p.getName()));
        }
        if (r.cards > 0) plugin.getSignManager().addCards(p.getUniqueId(), r.cards);
    }
}
