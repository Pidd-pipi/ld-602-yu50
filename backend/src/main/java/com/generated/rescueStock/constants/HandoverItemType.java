package com.generated.rescueStock.constants;

public final class HandoverItemType {
  public static final String INBOUND = "INBOUND";
  public static final String EXPIRE = "EXPIRE";
  public static final String EXCEPTION = "EXCEPTION";
  public static final String OTHER = "OTHER";

  private HandoverItemType() {}

  public static String text(String type) {
    return switch (type) {
      case INBOUND -> "待入库";
      case EXPIRE -> "临期批次";
      case EXCEPTION -> "异常库存";
      case OTHER -> "其他事项";
      default -> type;
    };
  }
}
