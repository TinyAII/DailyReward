package nl.tinyaii.dailyreward.command;

import nl.tinyaii.dailyreward.DailyRewardPlugin;
import nl.tinyaii.dailyreward.gui.CalendarMenu;
import nl.tinyaii.dailyreward.sign.SignData;
import nl.tinyaii.dailyreward.sign.SignService;
import nl.tinyaii.dailyreward.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SignCommand implements CommandExecutor, TabCompleter {
    private final DailyRewardPlugin plugin;
    private final SignService service;

    public SignCommand(DailyRewardPlugin plugin) {
        this.plugin = plugin;
        this.service = new SignService(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Messages msg = plugin.getMessages();

        // /签到 → 永远打开月历 GUI（签到/补签/领奖都在 GUI 里点）
        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage("控制台请用: /签到 排行"); return true; }
            if (!sender.hasPermission("daily.use")) { msg.send((Player) sender, "no-permission"); return true; }
            new CalendarMenu(plugin, (Player) sender).open();
            return true;
        }

        switch (args[0]) {
            case "信息": {
                if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
                if (!checkUse((Player) sender)) return true;
                SignData d = plugin.getSignManager().getOrCreate(((Player) sender).getUniqueId());
                ((Player) sender).sendMessage(msg.raw("info",
                        "{streak}", String.valueOf(d.streak),
                        "{total}", String.valueOf(d.total),
                        "{cards}", String.valueOf(d.cards)));
                return true;
            }
            case "排行": {
                if (!checkUse(sender)) return true;
                List<SignData> top = plugin.getSignManager().topByStreak(10);
                sender.sendMessage(Messages.color("&6==== 连签排行 ===="));
                int i = 1;
                for (SignData d : top) {
                    String name = nameOf(d.uuid);
                    sender.sendMessage(Messages.color("&7" + i++ + ". &e" + name
                            + " &f- 连签&a" + d.streak + "天 &7/ 累计&b" + d.total + "天"));
                }
                return true;
            }
            case "补签": {
                if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
                if (!checkUse((Player) sender)) return true;
                // 命令补签 = 自动补最近的一个缺口（GUI 可精确选日期）
                Player p = (Player) sender;
                SignData d = plugin.getSignManager().getOrCreate(p.getUniqueId());
                java.time.LocalDate target = null;
                int maxBack = plugin.getConfig().getInt("makeup.max-backfill-days", 3);
                for (int i = 1; i <= maxBack; i++) {
                    java.time.LocalDate cand = java.time.LocalDate.now().minusDays(i);
                    if (!plugin.getSignManager().isSigned(d, cand)) { target = cand; break; }
                }
                if (target == null) { msg.send(p, "already"); return true; }
                boolean ok = service.makeupDay(p, target);
                if (ok) new CalendarMenu(plugin, p).open();
                return true;
            }

            // ---- 管理 ----
            case "给卡": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /签到 给卡 <玩家> <数量>")); return true; }
                Player t = Bukkit.getPlayerExact(args[1]);
                int n;
                try { n = Integer.parseInt(args[2]); } catch (Exception e) { n = 0; }
                if (t == null || n <= 0) { sender.sendMessage(Messages.color("&c玩家需在线且数量>0。")); return true; }
                plugin.getSignManager().addCards(t.getUniqueId(), n);
                sender.sendMessage(Messages.color("&a已给 &e" + t.getName() + " &a发放 &e" + n + " &a张补签卡。"));
                return true;
            }
            case "重置": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 2) { sender.sendMessage(Messages.color("&c用法: /签到 重置 <在线玩家>")); return true; }
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage(Messages.color("&c玩家需在线（重置其内存数据）。")); return true; }
                plugin.getSignManager().getOrCreate(t.getUniqueId());
                // 直接重建对象即清零并保存
                plugin.getSignManager().reset(t.getUniqueId());
                sender.sendMessage(Messages.color("&a已重置 &e" + t.getName() + " &a的签到数据。"));
                return true;
            }
            case "重载": {
                if (!checkAdmin(sender)) return true;
                plugin.reloadAll();
                if (sender instanceof Player) msg.send((Player) sender, "reloaded");
                else sender.sendMessage(msg.raw("reloaded"));
                return true;
            }
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender s) {
        String[] lines = {
                "&6===== DailyReward 每日签到 =====",
                "&e/签到 &7- 每日签到（模式: " + plugin.modeName() + "）",
                "&e/签到 信息 &7- 连签/累计/补签卡",
                "&e/签到 排行 &7- 连签榜",
                "&e/签到 补签 &7- 用补签卡救连签",
                "&c--- 管理 ---",
                "&e/签到 给卡 <玩家> <数量>",
                "&e/签到 重置 <玩家>",
                "&e/签到 重载"
        };
        for (String l : lines) s.sendMessage(Messages.color(l));
    }

    private boolean checkUse(Player p) {
        if (p.hasPermission("daily.use")) return true;
        plugin.getMessages().send(p, "no-permission");
        return false;
    }

    private boolean checkUse(CommandSender s) {
        if (s instanceof Player) return checkUse((Player) s);
        return s.hasPermission("daily.use");
    }

    private boolean checkAdmin(CommandSender s) {
        if (s.hasPermission("daily.admin")) return true;
        if (s instanceof Player) plugin.getMessages().send((Player) s, "no-permission");
        else s.sendMessage(plugin.getMessages().raw("no-permission"));
        return false;
    }

    private String nameOf(java.util.UUID uuid) {
        var d = plugin.getSignManager().getOrCreate(uuid);
        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("信息", "排行", "补签"));
            if (sender.hasPermission("daily.admin")) subs.addAll(Arrays.asList("给卡", "重置", "重载"));
            for (String s : subs) if (s.startsWith(args[0])) out.add(s);
        } else if (args.length == 2 && Arrays.asList("给卡", "重置").contains(args[0])) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            }
        }
        return out;
    }
}
