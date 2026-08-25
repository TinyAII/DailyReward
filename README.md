# DailyReward 每日签到

轻量级每日签到插件：月历 GUI 签到、连签奖励递增、7 天大奖循环、补签卡救连签、累计里程碑、双签到模式（自然日 / 在线时长）。零硬依赖，装了 Economy 插件自动发金币。

![Version](https://img.shields.io/badge/version-1.0.0-blue) ![License](https://img.shields.io/badge/license-MIT-green) ![API](https://img.shields.io/badge/API-1.16%2B-orange)

## 功能特性

- **月历 GUI**：`/签到` 打开当月日历——已签绿玻璃✔、今天金色★、断签红玻璃（可点击补签）、未来灰色；底部实时统计连签/累计/补签卡
- **双签到模式**（config 一行切换）：
  - `date` — 自然日签到，每日 0 点刷新（默认）
  - `online` — 在线时长达标才能签（防挂机党白嫖，默认要求 30 分钟，可配）
- **连签递增**：奖励 = 基础 × (1 + (连签-1) × 步长)，默认每多连 1 天 +10%
- **7 天大奖循环**：第 7/14/21… 天基础奖励翻倍
- **补签卡**：断签后 3 天内可逐日补救（GUI 点红格子或 `/签到 补签`）；支持管理员发放、里程碑赠送、金币购买三种来源
- **里程碑成就**：累计 30 / 100 / 365 天领大奖（config 可改节点），达成时 GUI 宝箱图标亮起
- **Economy 联动**：检测到 TinyAII Economy 插件自动发金币（读取货币名），没装则只发物品，编译期零依赖
- **旧数据兼容**：v1.0 单点数据自动迁移进日期历史集合

## 命令

| 命令 | 说明 | 权限 |
|---|---|---|
| `/签到` | 打开月历 GUI（签到/补签/领奖全在 GUI 里点） | daily.use |
| `/签到 信息` | 查看连签/累计/补签卡 | daily.use |
| `/签到 排行` | 连签天数排行榜 | daily.use |
| `/签到 补签` | 快捷补最近一个缺口 | daily.use |
| `/签到 给卡 <玩家> <数量>` | 发放补签卡 | daily.admin |
| `/签到 重置 <玩家>` | 清空签到数据 | daily.admin |
| `/签到 重载` | 重载配置 | daily.admin |

权限默认值：`daily.use` 所有人、`daily.admin` OP。

## 配置示例

```yaml
settings:
  mode: date            # date=自然日 / online=在线时长
  online-minutes: 30    # online 模式当日要求分钟数
streak:
  step: 0.1             # 每多连1天 +10%
  cycle-days: 7         # 第7天大奖日翻倍
makeup:
  max-backfill-days: 3  # 最多倒补几天
  cost: 0               # 金币购买价（>0 开启金币购卡）
milestones: [30, 100, 365]
rewards:
  day-1: { money: 50, items: [{material: BREAD, amount: 8}] }
  day-7: { money: 250, items: [{material: DIAMOND, amount: 3}] }  # 大奖日实际×2
```

## 安装

1. 下载 `dailyreward-1.0.0.jar` 放入服务器 `plugins/` 目录
2. 重启服务器
3. 编辑 `plugins/DailyReward/config.yml` 自定义奖励表与模式

## 兼容性

- 支持核心：Spigot / Paper / Purpur / Leaves
- API 版本：1.16+（spigot-api 1.16.5 编译）
- Java：17+
- 前置依赖：无（Economy 为可选联动）

## 开源协议

MIT License

---

# DailyReward (English)

Lightweight daily sign-in plugin: calendar GUI, streak rewards, 7-day jackpot cycle, makeup cards, cumulative milestones, and dual sign-in modes (calendar-day / online-time). Zero hard dependencies; auto-grants money when TinyAII Economy is installed.

## Features

- **Calendar GUI**: signed days green, today gold, missed days red (clickable to make-up), future gray; live stats bar
- **Dual modes**: `date` (resets at midnight, default) or `online` (requires N minutes of playtime today, anti-AFK)
- **Streak scaling**: reward = base × (1 + (streak-1) × step), +10% per day by default
- **7-day jackpot**: base rewards doubled every 7th day of the cycle
- **Makeup cards**: fix missed days within 3 days via GUI or command; obtainable from admins, milestones, or purchase with in-game money
- **Milestones**: cumulative rewards at 30 / 100 / 365 days (configurable)
- **Economy integration**: reflection-based, zero compile dependency

## Compatibility

- Server: Spigot / Paper / Purpur / Leaves
- API version: 1.16+
- Java 17+
- Dependencies: none (optional Economy)

## License

MIT License

## Author

**TinyAII**
