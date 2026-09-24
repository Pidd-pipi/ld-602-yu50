package com.generated.rescueStock.constructors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.generated.rescueStock.models.HandoverItem;
import com.generated.rescueStock.models.HandoverSheet;

/** 值班交接单响应 DTO 构造器：领域模型不直接暴露给前端，字段统一 snake_case */
public final class ShiftHandoverDtoFactory {

  private ShiftHandoverDtoFactory() {}

  public static Map<String, Object> item(HandoverItem item) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", item.id);
    map.put("type", item.type);
    map.put("content", item.content);
    map.put("assignee", item.assignee);
    map.put("status", item.status);
    map.put("result_note", item.resultNote == null ? "" : item.resultNote);
    map.put("completed_at", item.completedAt == null ? null : item.completedAt.toString());
    return map;
  }

  public static Map<String, Object> sheet(HandoverSheet sheet) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", sheet.id);
    map.put("warehouse_id", sheet.warehouseId);
    map.put("manager_name", sheet.managerName);
    map.put("shift_start", sheet.shiftStart == null ? null : sheet.shiftStart.toString());
    map.put("shift_end", sheet.shiftEnd == null ? null : sheet.shiftEnd.toString());
    map.put("status", sheet.status);
    map.put("claimed_by", sheet.claimedBy);
    map.put("claimed_at", sheet.claimedAt == null ? null : sheet.claimedAt.toString());
    map.put("created_at", sheet.createdAt == null ? null : sheet.createdAt.toString());
    map.put("closed_at", sheet.closedAt == null ? null : sheet.closedAt.toString());
    List<Map<String, Object>> items = sheet.items.stream().map(ShiftHandoverDtoFactory::item).toList();
    map.put("items", items);
    return map;
  }
}
