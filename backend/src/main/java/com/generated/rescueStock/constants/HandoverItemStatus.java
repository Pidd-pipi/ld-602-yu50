package com.generated.rescueStock.constants;

import java.util.Map;

/** 交接事项处理状态：新值班员逐项处理，全部 DONE 后交接单才允许关闭。 */
public final class HandoverItemStatus {
  private HandoverItemStatus() {}

  public static final String PENDING = "PENDING";
  public static final String DONE = "DONE";

  public static final Map<String, String> TEXT = Map.of(
      PENDING, "待处理",
      DONE, "已处理");

  public static String textOf(String status) {
    return TEXT.getOrDefault(status, status);
  }
}
