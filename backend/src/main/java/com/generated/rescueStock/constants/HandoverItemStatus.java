package com.generated.rescueStock.constants;

public final class HandoverItemStatus {
  public static final String PENDING = "PENDING";
  public static final String DONE = "DONE";

  private HandoverItemStatus() {}

  public static String text(String status) {
    return DONE.equals(status) ? "已处理" : "待处理";
  }
}
