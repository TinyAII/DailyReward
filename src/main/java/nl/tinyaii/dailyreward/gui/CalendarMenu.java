package nl.tinyaii.dailyreward.gui;

import nl.tinyaii.dailyreward.DailyRewardPlugin;
import nl.tinyaii.dailyreward.sign.SignData;
import nl.tinyaii.dailyreward.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 月历签到 GUI（只读展示+点击动作）：
 * 已签绿玻璃✅ / 今天金色(可签)/绿色(已签) / 可补红玻璃❌(点击补签) / 超期深灰 / 未来灰。
 */
public class CalendarMenu {

    public static final String TITLE = ChatColor.DARK_GRAY + "每日签到";
    private static final int SIZE = 54;
    private static final int MAX_BACK = 3; // 与 config makeup.max-backfill-days 默认一致（渲染用）

    private final DailyRewardPlugin plugin;
    private final Player player;

    public CalendarMenu(DailyRewardPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        SignData d = plugin.getSignManager().getOrCreate(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(new MenuHolder(0, 1), SIZE, TITLE);

        YearMonth month = YearMonth.now();
        LocalDate today = LocalDate.now();
        int firstDow = month.atDay(1).getDayOfWeek().getValue();
        int maxBack = plugin.getConfig().getInt("makeup.max-backfill-days", MAX_BACK);

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            int slot = (firstDow - 1) + (day - 1);
            if (slot >= 45) break;
            LocalDate date = month.atDay(day);
            boolean signed = plugin.getSignManager().isSigned(d, date);

            Material mat; String name; List<String> lore = new ArrayList<>();

            if (date.isAfter(today)) {
                mat = Material.GRAY_STAINED_GLASS_PANE; name = "&7" + day + "日";
                lore.add("&8未来");
            } else if (signed) {
                mat = Material.LIME_STAINED_GLASS_PANE; name = "&a✔ " + day + "日 已签";
            } else if (date.equals(today)) {
                if ("online".equalsIgnoreCase(plugin.getConfig().getString("settings.mode", "date"))
                        && !plugin.getSignService().onlineReady(player)) {
                    int need = plugin.getConfig().getInt("settings.online-minutes", 30);
                    int have = plugin.getTimeTracker().onlineMinutesToday(player);
                    mat = Material.CLOCK; name = "&e" + day + "日 今天";
                    lore.add("&7在线时长不足: &e" + have + "/" + need + " 分钟");
                } else {
                    mat = Material.GOLD_BLOCK; name = "&e★ " + day + "日 今天";
                    lore.add("&a点击签到！");
                }
            } else {
                long ago = java.time.temporal.ChronoUnit.DAYS.between(date, today);
                if (ago <= maxBack) {
                    mat = Material.RED_STAINED_GLASS_PANE; name = "&c✖ " + day + "日 断签";
                    lore.add("&e点击补签 &7(消耗1张卡)");
                    lore.add("&7当前补签卡: &f" + d.cards + " 张");
                } else {
                    mat = Material.BLACK_STAINED_GLASS_PANE; name = "&8✖ " + day + "日";
                    lore.add("&8超出补签范围");
                }
            }
            // 附日期 ISO（供点击定位）
            inv.setItem(slot, named(mat, name, lore, date.toString()));
        }

        // 底部信息栏
        inv.setItem(48, named(Material.BOOK, "&6统计", Arrays.asList(
                Messages.color("&7当前连签: &e" + d.streak + " 天"),
                Messages.color("&7累计签到: &e" + d.total + " 天"),
                Messages.color("&7补签卡: &e" + d.cards + " 张"),
                Messages.color("&7模式: &f" + plugin.modeName()))));
        inv.setItem(49, named(Material.BARRIER, "&c关闭", new ArrayList<>()));

        boolean claimable = false;
        for (int m : plugin.getRewardConfig().milestoneKeys()) {
            if (d.total >= m && !d.milestonesClaimed.contains(m)) { claimable = true; break; }
        }
        String actionName;
        List<String> actionLore = new ArrayList<>();
        Material actionMat;
        if (!d.signedToday()) {
            actionMat = Material.GOLD_BLOCK;
            actionName = "&a▶ 立即签到";
            if ("online".equalsIgnoreCase(plugin.getConfig().getString("settings.mode", "date"))) {
                int need = plugin.getConfig().getInt("settings.online-minutes", 30);
                int have = plugin.getTimeTracker().onlineMinutesToday(player);
                actionLore.add(Messages.color("&7今日在线: &e" + have + "/" + need + " 分钟"));
            }
        } else if (claimable) {
            actionMat = Material.CHEST_MINECART;
            actionName = "&6▶ 领取里程碑奖励";
        } else {
            actionMat = Material.LIME_DYE;
            actionName = "&a今日已签，明天再来～";
            actionLore.add(Messages.color("&7当前连签 &e" + d.streak + " &7天"));
        }
        inv.setItem(50, named(actionMat, actionName, actionLore));
        player.openInventory(inv);
    }

    private ItemStack named(Material mat, String name, List<String> lore) {
        return named(mat, name, lore, null);
    }

    /** dateIso 塞进 lore 尾部隐藏段，点击时解析出目标日期 */
    private ItemStack named(Material mat, String name, List<String> lore, String dateIso) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(name));
            List<String> out = new ArrayList<>();
            for (String l : lore) out.add(Messages.color(l));
            if (dateIso != null) out.add(ChatColor.BLACK + "" + ChatColor.DARK_GRAY + dateIso); // 隐形标记
            meta.setLore(out);
            it.setItemMeta(meta);
        }
        return it;
    }
}
