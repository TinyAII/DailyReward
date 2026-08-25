package nl.tinyaii.dailyreward.gui;

import nl.tinyaii.dailyreward.DailyRewardPlugin;
import nl.tinyaii.dailyreward.sign.SignService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;

/**
 * 月历 GUI 点击：红格子→补指定日；金色今日/底部按钮→签到；宝箱车→领里程碑。
 */
public class MenuListener implements Listener {

    private final DailyRewardPlugin plugin;

    public MenuListener(DailyRewardPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof MenuHolder)) return;
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        if (e.getClick() != ClickType.LEFT) return;

        int slot = e.getRawSlot();
        SignService service = plugin.getSignService();

        if (slot == 49) { p.closeInventory(); return; }

        // 底部动作按钮（48=统计只读 50=动作）
        if (slot == 50) {
            var d = plugin.getSignManager().getOrCreate(p.getUniqueId());
            if (!d.signedToday()) {
                p.closeInventory();
                service.sign(p);
                BukkitRefresh.openLater(plugin, p);
            } else {
                boolean claimable = false;
                for (int m : plugin.getRewardConfig().milestoneKeys()) {
                    if (d.total >= m && !d.milestonesClaimed.contains(m)) { claimable = true; break; }
                }
                if (claimable) {
                    service.claimMilestones(p);
                    BukkitRefresh.openLater(plugin, p);
                }
            }
            return;
        }

        // 日期格子（0~44）：从 lore 隐形标记解析日期
        if (slot < 45) {
            LocalDate date = parseDateIso(clicked);
            if (date == null || !date.isBefore(java.time.LocalDate.now())) return;
            boolean signed = plugin.getSignManager().isSigned(
                    plugin.getSignManager().getOrCreate(p.getUniqueId()), date);
            if (signed) return;   // 已签的格子点了没反应

            boolean ok = service.makeupDay(p, date);
            if (ok) BukkitRefresh.openLater(plugin, p);   // 成功后刷新 GUI 显示最新状态
        }
    }

    /** 从物品 lore 尾部隐形标记提取 yyyy-MM-dd */
    private LocalDate parseDateIso(ItemStack item) {
        if (item.getItemMeta() == null || item.getItemMeta().getLore() == null) return null;
        for (String line : item.getItemMeta().getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped != null && stripped.matches("\\d{4}-\\d{2}-\\d{2}")) {
                try { return LocalDate.parse(stripped); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /** 下 tick 重开 GUI 刷新状态 */
    static class BukkitRefresh {
        static void openLater(DailyRewardPlugin plugin, Player p) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin,
                    () -> new CalendarMenu(plugin, p).open());
        }
    }
}
