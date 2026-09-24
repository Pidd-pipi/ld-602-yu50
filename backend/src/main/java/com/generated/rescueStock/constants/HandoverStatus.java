package com.generated.rescueStock.constants;

public final class HandoverStatus {
  public static final String PENDING = "PENDING";
  public static final String CLAIMED = "CLAIMED";
  public static final String CLOSED = "CLOSED";

  private HandoverStatus() {}

  public static String text(String status) {
    return switch (status) {
      case PENDING -> "待领取";
      case CLAIMED -> "处理中";
      case CLOSED -> "已关闭";
      default -> status;
    };
  }
}
