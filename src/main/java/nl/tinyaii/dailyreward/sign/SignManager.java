package nl.tinyaii.dailyreward.sign;

import nl.tinyaii.dailyreward.DailyRewardPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 签到管理：日期历史集合存储，单入口锁，变动即落盘。
 */
public class SignManager {
    private final DailyRewardPlugin plugin;
    private final Map<UUID, SignData> data = new HashMap<>();
    private File file;
    private final Object lock = new Object();

    public SignManager(DailyRewardPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        synchronized (lock) {
            data.clear();
            file = new File(plugin.getDataFolder(), "data.yml");
            if (!file.exists()) return;
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection root = yml.getConfigurationSection("players");
            if (root == null) return;
            for (String key : root.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    ConfigurationSection s = root.getConfigurationSection(key);
                    if (s == null) continue;
                    SignData d = new SignData(uuid);
                    d.history.addAll(s.getStringList("history"));
                    d.total = s.getInt("total", 0);
                    d.cards = s.getInt("cards", 0);
                    d.onlineSecondsToday = s.getLong("online-seconds", 0);
                    d.onlineDate = s.getString("online-date", "");
                    d.milestonesClaimed.addAll(s.getIntegerList("milestones-claimed"));
                    d.streak = calcStreak(d);
                    // 兼容旧版数据（last-date 单点 → 迁移进 history）
                    String legacy = s.getString("last-date", "");
                    if (!legacy.isEmpty()) {
                        d.history.add(legacy);
                        d.streak = Math.max(d.streak, 1);
                        if (d.total == 0) d.total = 1;
                    }
                    data.put(uuid, d);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    /** 从 history 推导当前连签：从今天（或昨天）往回连续计数 */
    public int calcStreak(SignData d) {
        LocalDate cursor = LocalDate.now();
        if (!d.history.contains(cursor.toString())) {
            cursor = cursor.minusDays(1);      // 今天还没签，从昨天起算
            if (!d.history.contains(cursor.toString())) return 0;
        }
        int n = 0;
        while (d.history.contains(cursor.toString())) {
            n++;
            cursor = cursor.minusDays(1);
        }
        return n;
    }

    public void save() {
        synchronized (lock) {
            YamlConfiguration yml = new YamlConfiguration();
            for (SignData d : data.values()) {
                String base = "players." + d.uuid + ".";
                yml.set(base + "history", new ArrayList<>(d.history));
                yml.set(base + "streak", d.streak);
                yml.set(base + "total", d.total);
                yml.set(base + "cards", d.cards);
                yml.set(base + "online-seconds", d.onlineSecondsToday);
                yml.set(base + "online-date", d.onlineDate);
                yml.set(base + "milestones-claimed", new ArrayList<>(d.milestonesClaimed));
            }
            try {
                if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
                yml.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("保存 data.yml 失败: " + e.getMessage());
            }
        }
    }

    public SignData getOrCreate(UUID uuid) {
        synchronized (lock) {
            return data.computeIfAbsent(uuid, SignData::new);
        }
    }

    public int size() { return data.size(); }

    // ---------- 连签周期 ----------

    public int cycleDay(SignData d) {
        int cycle = Math.max(1, plugin.getConfig().getInt("streak.cycle-days", 7));
        return ((Math.max(1, d.streak) - 1) % cycle) + 1;
    }

    // ---------- 签到/补签核心 ----------

    /**
     * 标记某天已签并重算连签。
     * @return 该天之前的连签基数（用于奖励倍率）
     */
    public int markSigned(SignData d, LocalDate date) {
        boolean hadYesterday = d.history.contains(date.minusDays(1).toString());
        d.history.add(date.toString());
        d.total++;
        d.streak = calcStreak(d);
        save();
        return hadYesterday ? d.streak - 1 : 0; // 本次签到前的连签
    }

    /**
     * 补指定缺口日（消耗 1 张卡）。
     * @return null=成功；"no-card"；"too-far"
     */
    public String makeupDay(SignData d, LocalDate gap) {
        if (d.history.contains(gap.toString())) return "already";
        int maxBack = plugin.getConfig().getInt("makeup.max-backfill-days", 3);
        long ago = java.time.temporal.ChronoUnit.DAYS.between(gap, LocalDate.now());
        if (ago < 1 || ago > maxBack) return "too-far";
        if (d.cards <= 0) return "no-card";

        d.cards--;
        d.history.add(gap.toString());
        d.total++;
        d.streak = calcStreak(d);
        save();
        return null;
    }

    /** 查询某日是否已签（GUI 渲染用） */
    public boolean isSigned(SignData d, LocalDate date) {
        return d.history.contains(date.toString());
    }

    public void addCards(UUID uuid, int n) {
        SignData d = getOrCreate(uuid);
        d.cards += n;
        save();
    }

    /** 重置某玩家签到数据 */
    public void reset(UUID uuid) {
        synchronized (lock) {
            data.put(uuid, new SignData(uuid));
            save();
        }
    }

    public List<SignData> topByStreak(int limit) {
        List<SignData> list = new ArrayList<>(data.values());
        list.sort(Comparator.comparingInt((SignData d) -> d.streak).reversed());
        return list.size() > limit ? list.subList(0, limit) : list;
    }
}
