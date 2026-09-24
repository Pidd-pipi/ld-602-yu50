package com.generated.rescueStock.constants;

import java.util.List;
import java.util.Map;

/** 交接事项类别：待处理入库、临期批次、异常库存及其他。 */
public final class HandoverItemCategory {
  private HandoverItemCategory() {}

  public static final String INBOUND_PENDING = "INBOUND_PENDING";
  public static final String EXPIRING_BATCH = "EXPIRING_BATCH";
  public static final String ABNORMAL_INVENTORY = "ABNORMAL_INVENTORY";
  public static final String OTHER = "OTHER";

  public static final List<String> ALL = List.of(INBOUND_PENDING, EXPIRING_BATCH, ABNORMAL_INVENTORY, OTHER);

  public static final Map<String, String> TEXT = Map.of(
      INBOUND_PENDING, "待入库",
      EXPIRING_BATCH, "临期批次",
      ABNORMAL_INVENTORY, "异常库存",
      OTHER, "其他事项");

  public static String textOf(String category) {
    return TEXT.getOrDefault(category, category);
  }
}
