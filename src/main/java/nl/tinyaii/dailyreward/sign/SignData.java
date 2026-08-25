package nl.tinyaii.dailyreward.sign;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家签到数据模型。核心为签到日期历史集合（支持精确月历显示与逐日补签）。
 */
public class SignData {
    public final UUID uuid;
    /** 签到日期历史（yyyy-MM-dd 集合）—— 月历显示与连签计算的真相来源 */
    public final Set<String> history = new HashSet<>();
    /** 当前连签天数（由 history 推导缓存） */
    public int streak = 0;
    /** 累计签到天数（里程碑用，不清零） */
    public int total = 0;
    /** 补签卡张数 */
    public int cards = 0;
    /** 已领取的里程碑节点 */
    public final Set<Integer> milestonesClaimed = new HashSet<>();
    /** online 模式：当日累计在线秒数（跨日清零） */
    public long onlineSecondsToday = 0L;
    public String onlineDate = "";

    public SignData(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean signedToday() {
        return history.contains(LocalDate.now().toString());
    }

    public boolean signedYesterday() {
        return history.contains(LocalDate.now().minusDays(1).toString());
    }
}
