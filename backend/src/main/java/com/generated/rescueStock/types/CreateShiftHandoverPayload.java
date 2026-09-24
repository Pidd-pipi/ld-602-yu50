package com.generated.rescueStock.types;

import java.util.List;

/**
 * 创建值班交接单请求。
 * 选择当班负责人和时段，并逐条登记事项与接手人。
 */
public record CreateShiftHandoverPayload(
    Long warehouse_id,
    String shift_owner,
    String claimed_by,
    String shift_start_at,
    String shift_end_at,
    String remark,
    List<HandoverItemPayload> items) {

  /** 交接事项条目。 */
  public record HandoverItemPayload(
      String category,
      String content,
      String assignee,
      String batch_no) {}
}
