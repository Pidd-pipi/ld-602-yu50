package com.generated.rescueStock.services;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.generated.rescueStock.constants.ErrorCodes;
import com.generated.rescueStock.constants.ErrorMessages;
import com.generated.rescueStock.constants.HandoverItemStatus;
import com.generated.rescueStock.constants.HandoverItemType;
import com.generated.rescueStock.constants.HandoverStatus;
import com.generated.rescueStock.constants.LogTemplates;
import com.generated.rescueStock.models.HandoverItem;
import com.generated.rescueStock.models.HandoverSheet;
import com.generated.rescueStock.repositories.ShiftHandoverRepository;
import com.generated.rescueStock.types.HandoverCreatePayload;
import com.generated.rescueStock.types.HandoverItemPayload;

@Service
public class ShiftHandoverService {

  private final ShiftHandoverRepository repo;

  public ShiftHandoverService(ShiftHandoverRepository repo) {
    this.repo = repo;
  }

  public List<HandoverSheet> list(Long warehouseId) {
    return repo.findAll().stream()
        .filter(sheet -> warehouseId == null || warehouseId.equals(sheet.warehouseId))
        .toList();
  }

  public HandoverSheet create(HandoverCreatePayload payload) {
    List<String> validationErrors = payload.validate();
    if (!validationErrors.isEmpty()) {
      throw new HandoverBusinessException(ErrorCodes.VALIDATION_FAILED, String.join("；", validationErrors), Map.of());
    }
    for (HandoverItemPayload itemPayload : payload.items()) {
      if (!isKnownItemType(itemPayload.type())) {
        throw new HandoverBusinessException(
            ErrorCodes.VALIDATION_FAILED, "未知的交接事项类型：" + itemPayload.type(), Map.of());
      }
    }

    HandoverSheet sheet = new HandoverSheet();
    sheet.warehouseId = payload.warehouseId();
    sheet.managerName = payload.managerName().trim();
    sheet.shiftStart = payload.shiftStart();
    sheet.shiftEnd = payload.shiftEnd();
    sheet.status = HandoverStatus.PENDING;
    sheet.createdAt = Instant.now();

    for (int index = 0; index < payload.items().size(); index++) {
      HandoverItemPayload source = payload.items().get(index);
      sheet.items.add(new HandoverItem(
          (long) index + 1,
          source.type(),
          source.content().trim(),
          source.assignee().trim(),
          HandoverItemStatus.PENDING));
    }

    // 冲突校验在写入前执行：同仓库半开区间重叠且对方仍有效（未关闭）时拒绝，原单不动
    HandoverSheet conflict = findOverlap(sheet.warehouseId, sheet.shiftStart, sheet.shiftEnd, null);
    if (conflict != null) {
      throw conflictError(conflict);
    }
    repo.save(sheet);
    return sheet;
  }

  public HandoverSheet claim(Long id, String claimedByRaw) {
    String claimedBy = claimedByRaw == null ? "" : claimedByRaw.trim();
    if (claimedBy.isBlank()) {
      throw new HandoverBusinessException(ErrorCodes.VALIDATION_FAILED, "请填写领取人姓名", Map.of());
    }
    HandoverSheet sheet = requireSheet(id);
    if (!HandoverStatus.PENDING.equals(sheet.status)) {
      throw new HandoverBusinessException(
          ErrorCodes.HANDOVER_NOT_PENDING,
          ErrorMessages.HANDOVER_NOT_PENDING,
          Map.of("id", id, "status", sheet.status));
    }
    sheet.status = HandoverStatus.CLAIMED;
    sheet.claimedBy = claimedBy;
    sheet.claimedAt = Instant.now();
    return sheet;
  }

  public HandoverSheet completeItem(Long id, Long itemId, String noteRaw) {
    String note = noteRaw == null ? "" : noteRaw.trim();
    if (note.isBlank()) {
      throw new HandoverBusinessException(ErrorCodes.VALIDATION_FAILED, "请填写事项处理结果", Map.of());
    }
    HandoverSheet sheet = requireSheet(id);
    if (!HandoverStatus.CLAIMED.equals(sheet.status)) {
      throw new HandoverBusinessException(
          ErrorCodes.HANDOVER_NOT_PENDING,
          "只有处理中的交接单可以逐项登记处理结果",
          Map.of("id", id, "status", sheet.status));
    }
    HandoverItem item = sheet.items.stream()
        .filter(candidate -> candidate.id.equals(itemId))
        .findFirst()
        .orElseThrow(() -> new HandoverBusinessException(
            ErrorCodes.HANDOVER_NOT_FOUND, ErrorMessages.HANDOVER_NOT_FOUND, Map.of("id", id, "item_id", itemId)));
    item.status = HandoverItemStatus.DONE;
    item.resultNote = note;
    item.completedAt = Instant.now();
    return sheet;
  }

  public HandoverSheet close(Long id) {
    HandoverSheet sheet = requireSheet(id);
    if (!HandoverStatus.CLAIMED.equals(sheet.status)) {
      throw new HandoverBusinessException(
          ErrorCodes.HANDOVER_NOT_PENDING,
          "只有处理中的交接单可以关闭",
          Map.of("id", id, "status", sheet.status));
    }
    long pendingCount = sheet.items.stream().filter(item -> !HandoverItemStatus.DONE.equals(item.status)).count();
    if (pendingCount > 0) {
      throw new HandoverBusinessException(
          ErrorCodes.HANDOVER_ITEM_PENDING,
          ErrorMessages.HANDOVER_ITEM_PENDING,
          Map.of("id", id, "count", pendingCount));
    }
    sheet.status = HandoverStatus.CLOSED;
    sheet.closedAt = Instant.now();
    return sheet;
  }

  public String logTemplate(String action) {
    return switch (action) {
      case "create" -> LogTemplates.HANDOVER_CREATE;
      case "claim" -> LogTemplates.HANDOVER_CLAIM;
      case "item" -> LogTemplates.HANDOVER_ITEM_COMPLETE;
      case "close" -> LogTemplates.HANDOVER_CLOSE;
      default -> LogTemplates.UPDATE;
    };
  }

  private HandoverSheet requireSheet(Long id) {
    return repo.findById(id).orElseThrow(() -> new HandoverBusinessException(
        ErrorCodes.HANDOVER_NOT_FOUND, ErrorMessages.HANDOVER_NOT_FOUND, Map.of("id", id)));
  }

  private boolean isKnownItemType(String type) {
    return HandoverItemType.INBOUND.equals(type)
        || HandoverItemType.EXPIRE.equals(type)
        || HandoverItemType.EXCEPTION.equals(type)
        || HandoverItemType.OTHER.equals(type);
  }

  /** 半开区间 [start, end) 与同仓库任何有效（非 CLOSED）交接单重叠判定 */
  private HandoverSheet findOverlap(Long warehouseId, Instant start, Instant end, Long excludeId) {
    return repo.findAll().stream()
        .filter(sheet -> warehouseId.equals(sheet.warehouseId))
        .filter(sheet -> excludeId == null || !excludeId.equals(sheet.id))
        .filter(sheet -> !HandoverStatus.CLOSED.equals(sheet.status))
        .filter(sheet -> start.isBefore(sheet.shiftEnd) && sheet.shiftStart.isBefore(end))
        .findFirst()
        .orElse(null);
  }

  private HandoverBusinessException conflictError(HandoverSheet conflict) {
    Map<String, Object> meta = new HashMap<>();
    meta.put("id", conflict.id);
    meta.put("conflict_id", conflict.id);
    meta.put("conflict_status", conflict.status);
    meta.put("conflict_start", conflict.shiftStart.toString());
    meta.put("conflict_end", conflict.shiftEnd.toString());
    return new HandoverBusinessException(ErrorCodes.HANDOVER_SHIFT_CONFLICT, ErrorMessages.HANDOVER_SHIFT_CONFLICT, meta);
  }
}
