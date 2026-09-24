package com.generated.rescueStock.types;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public record HandoverCreatePayload(
    @JsonProperty("warehouse_id") Long warehouseId,
    @JsonProperty("manager_name") String managerName,
    @JsonProperty("shift_start") Instant shiftStart,
    @JsonProperty("shift_end") Instant shiftEnd,
    @JsonProperty("items") List<HandoverItemPayload> items) {

  public List<String> validate() {
    List<String> errors = new ArrayList<>();
    if (warehouseId == null) errors.add("请选择交接仓库");
    if (managerName == null || managerName.isBlank()) errors.add("请填写当班负责人");
    if (shiftStart == null || shiftEnd == null) {
      errors.add("请选择交接时段的开始与结束时间");
    } else if (!shiftEnd.isAfter(shiftStart)) {
      errors.add("时段结束时间必须晚于开始时间");
    }
    if (items == null || items.isEmpty()) {
      errors.add("至少逐条登记一条交接事项");
    } else {
      for (int index = 0; index < items.size(); index++) {
        for (String message : items.get(index).validate()) {
          errors.add("第 " + (index + 1) + " 条事项：" + message);
        }
      }
    }
    return errors;
  }
}
