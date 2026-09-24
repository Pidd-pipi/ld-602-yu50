package com.generated.rescueStock.constructors;

import java.util.LinkedHashMap;
import java.util.Map;
import com.generated.rescueStock.models.HandoverItem;
import com.generated.rescueStock.models.ShiftHandover;

/** 值班交接单 / 交接事项响应 DTO 构造器：service 与 controller 不得各自散写响应结构。 */
public final class ShiftHandoverDtoFactory {
  private ShiftHandoverDtoFactory() {}

  public static Map<String, Object> itemDto(HandoverItem item) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", item.id);
    map.put("handover_id", item.handover_id);
    map.put("category", item.category);
    map.put("content", item.content);
    map.put("assignee", item.assignee);
    map.put("batch_no", item.batch_no);
    map.put("status", item.status);
    map.put("processed_at", item.processed_at);
    map.put("process_note", item.process_note);
    map.put("sort_no", item.sort_no);
    return map;
  }

  public static Map<String, Object> handoverDto(ShiftHandover handover) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", handover.id);
    map.put("warehouse_id", handover.warehouse_id);
    map.put("shift_owner", handover.shift_owner);
    map.put("claimed_by", handover.claimed_by);
    map.put("shift_start_at", handover.shift_start_at);
    map.put("shift_end_at", handover.shift_end_at);
    map.put("remark", handover.remark);
    map.put("status", handover.status);
    map.put("created_at", handover.created_at);
    map.put("claimed_at", handover.claimed_at);
    map.put("closed_at", handover.closed_at);
    map.put("items", handover.items.stream().map(ShiftHandoverDtoFactory::itemDto).toList());
    return map;
  }

  /** 构造标准错误响应体：控制器在包装业务异常时统一使用。 */
  public static Map<String, Object> errorDto(String code, String message) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("code", code);
    map.put("message", message);
    return map;
  }
}
