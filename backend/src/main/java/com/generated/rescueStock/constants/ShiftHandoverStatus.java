package com.generated.rescueStock.constants;

import java.util.List;
import java.util.Map;

/** 值班交接单状态：待领取 / 处理中（已领取）/ 已关闭。只有前两种属于“有效交接单”，参与同一仓库的时段重叠校验。 */
public final class ShiftHandoverStatus {
  private ShiftHandoverStatus() {}

  public static final String PENDING_CLAIM = "PENDING_CLAIM";
  public static final String CLAIMED = "CLAIMED";
  public static final String CLOSED = "CLOSED";

  /** 有效交接单：未关闭的单据，同一仓库重叠时段只允许存在一张。 */
  public static final List<String> ACTIVE_STATUSES = List.of(PENDING_CLAIM, CLAIMED);

  public static final Map<String, String> TEXT = Map.of(
      PENDING_CLAIM, "待领取",
      CLAIMED, "处理中",
      CLOSED, "已关闭");

  public static String textOf(String status) {
    return TEXT.getOrDefault(status, status);
  }
}
