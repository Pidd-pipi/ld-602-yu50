package com.generated.rescueStock.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.generated.rescueStock.constants.ErrorCodes;
import com.generated.rescueStock.constants.ErrorMessages;
import com.generated.rescueStock.constants.HandoverItemCategory;
import com.generated.rescueStock.constants.HandoverItemStatus;
import com.generated.rescueStock.constants.LogTemplates;
import com.generated.rescueStock.constants.ShiftHandoverStatus;
import com.generated.rescueStock.constructors.ShiftHandoverDtoFactory;
import com.generated.rescueStock.models.HandoverItem;
import com.generated.rescueStock.models.ShiftHandover;
import com.generated.rescueStock.repositories.ShiftHandoverRepository;
import com.generated.rescueStock.types.ClaimShiftHandoverPayload;
import com.generated.rescueStock.types.CloseShiftHandoverPayload;
import com.generated.rescueStock.types.CreateShiftHandoverPayload;
import com.generated.rescueStock.types.ProcessHandoverItemPayload;

/** 值班交接业务：创建/领取/逐项处理/关闭，并在写入前完成时段重叠与状态校验。 */
@Service
public class ShiftHandoverService {
  private static final Logger audit = LoggerFactory.getLogger("AUDIT");
  private static final DateTimeFormatter CANONICAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private final ShiftHandoverRepository repo;

  public ShiftHandoverService(ShiftHandoverRepository repo) {
    this.repo = repo;
  }

  /** 按仓库回看交接单（不传 warehouse_id 时返回全部），重开页面后仍可查询。 */
  public List<Map<String, Object>> list(Long warehouseId) {
    return repo.findAll().stream()
        .filter(h -> warehouseId == null || warehouseId.equals(h.warehouse_id))
        .map(ShiftHandoverDtoFactory::handoverDto)
        .toList();
  }

  public Map<String, Object> detail(Long id) {
    return ShiftHandoverDtoFactory.handoverDto(requireHandover(id));
  }

  /** 创建交接单：同一仓库重叠时段若已有有效交接单则拒绝，原单与库存不动。 */
  public synchronized Map<String, Object> create(CreateShiftHandoverPayload payload) {
    validateCreate(payload);
    LocalDateTime start = parseTime(payload.shift_start_at(), "shift_start_at");
    LocalDateTime end = parseTime(payload.shift_end_at(), "shift_end_at");
    if (!end.isAfter(start)) {
      throw new ShiftHandoverException(ErrorCodes.VALIDATION_FAILED, "值守结束时间必须晚于开始时间。");
    }

    ShiftHandover conflict = findOverlappingActive(payload.warehouse_id(), start, end, null);
    if (conflict != null) {
      String reason = String.format(ErrorMessages.HANDOVER_SHIFT_CONFLICT,
          "#" + conflict.id + "（" + ShiftHandoverStatus.textOf(conflict.status)
              + "，" + conflict.shift_start_at + " ~ " + conflict.shift_end_at + "）");
      audit.warn("{} warehouse={} reason={}", LogTemplates.SHIFT_HANDOVER_REJECTED, payload.warehouse_id(), reason);
      throw ShiftHandoverException.conflict(reason);
    }

    LocalDateTime now = LocalDateTime.now();
    ShiftHandover handover = new ShiftHandover();
    handover.warehouse_id = payload.warehouse_id();
    handover.shift_owner = payload.shift_owner().trim();
    handover.claimed_by = payload.claimed_by() == null || payload.claimed_by().isBlank() ? null : payload.claimed_by().trim();
    handover.shift_start_at = start.format(CANONICAL);
    handover.shift_end_at = end.format(CANONICAL);
    handover.remark = payload.remark();
    handover.status = ShiftHandoverStatus.PENDING_CLAIM;
    handover.created_at = now.format(CANONICAL);

    int sortNo = 1;
    for (CreateShiftHandoverPayload.HandoverItemPayload itemPayload : payload.items()) {
      validateItem(itemPayload);
      HandoverItem item = new HandoverItem();
      item.id = repo.nextItemId();
      item.handover_id = handover.id;
      item.category = itemPayload.category();
      item.content = itemPayload.content().trim();
      item.assignee = itemPayload.assignee().trim();
      item.batch_no = itemPayload.batch_no();
      item.status = HandoverItemStatus.PENDING;
      item.sort_no = sortNo++;
      handover.items.add(item);
    }

    repo.save(handover);
    audit.info("{} id={} warehouse={} owner={} items={}",
        LogTemplates.SHIFT_HANDOVER_CREATE, handover.id, handover.warehouse_id, handover.shift_owner, handover.items.size());
    return ShiftHandoverDtoFactory.handoverDto(handover);
  }

  /** 新值班员领取：仅待领取单据可领取，已领取/已关闭时提示原因，且不改动原单与库存。 */
  public synchronized Map<String, Object> claim(Long id, ClaimShiftHandoverPayload payload) {
    ShiftHandover handover = requireHandover(id);
    if (!ShiftHandoverStatus.PENDING_CLAIM.equals(handover.status)) {
      String reason = String.format(ErrorMessages.HANDOVER_NOT_CLAIMABLE,
          handover.claimed_by == null ? "其他值班员" : handover.claimed_by,
          ShiftHandoverStatus.textOf(handover.status));
      audit.warn("{} id={} status={}", LogTemplates.SHIFT_HANDOVER_REJECTED, id, handover.status);
      throw ShiftHandoverException.notClaimable(reason);
    }
    if (payload == null || payload.claimed_by() == null || payload.claimed_by().isBlank()) {
      throw new ShiftHandoverException(ErrorCodes.VALIDATION_FAILED, "请填写领取交接单的值班员姓名。");
    }
    handover.claimed_by = payload.claimed_by().trim();
    handover.status = ShiftHandoverStatus.CLAIMED;
    handover.claimed_at = LocalDateTime.now().format(CANONICAL);
    repo.save(handover);
    audit.info("{} id={} claimed_by={}", LogTemplates.SHIFT_HANDOVER_CLAIM, id, handover.claimed_by);
    return ShiftHandoverDtoFactory.handoverDto(handover);
  }

  /** 逐项处理事项。仅处理中的交接单允许处理；处理动作只改事项状态，不回写库存批次。 */
  public synchronized Map<String, Object> processItem(Long id, Long itemId, ProcessHandoverItemPayload payload) {
    ShiftHandover handover = requireHandover(id);
    if (!ShiftHandoverStatus.CLAIMED.equals(handover.status)) {
      throw new ShiftHandoverException(ErrorCodes.HANDOVER_NOT_CLAIMABLE,
          "交接单当前状态为「" + ShiftHandoverStatus.textOf(handover.status) + "」，领取后才能逐项处理事项。");
    }
    HandoverItem item = handover.items.stream().filter(i -> i.id.equals(itemId)).findFirst()
        .orElseThrow(() -> new ShiftHandoverException(ErrorCodes.HANDOVER_ITEM_NOT_FOUND, ErrorMessages.HANDOVER_ITEM_NOT_FOUND));

    String target = payload == null ? HandoverItemStatus.DONE : payload.status();
    if (!HandoverItemStatus.PENDING.equals(target) && !HandoverItemStatus.DONE.equals(target)) {
      throw new ShiftHandoverException(ErrorCodes.VALIDATION_FAILED, "事项状态只能是 PENDING 或 DONE。");
    }
    item.status = target;
    if (HandoverItemStatus.DONE.equals(target)) {
      item.processed_at = LocalDateTime.now().format(CANONICAL);
      item.process_note = payload.process_note();
    } else {
      item.processed_at = null;
      item.process_note = null;
    }
    repo.save(handover);
    audit.info("{} handover={} item={} status={} by={}",
        LogTemplates.SHIFT_HANDOVER_ITEM_PROCESS, id, itemId, item.status, handover.claimed_by);
    return ShiftHandoverDtoFactory.handoverDto(handover);
  }

  /** 关闭交接单：全部事项处理完成才允许关闭，否则提示剩余未处理条数，原单与库存不动。 */
  public synchronized Map<String, Object> close(Long id, CloseShiftHandoverPayload payload) {
    ShiftHandover handover = requireHandover(id);
    long pendingCount = handover.items.stream().filter(i -> HandoverItemStatus.PENDING.equals(i.status)).count();
    if (pendingCount > 0) {
      String reason = String.format(ErrorMessages.HANDOVER_ITEMS_PENDING, pendingCount);
      audit.warn("{} id={} pending={}", LogTemplates.SHIFT_HANDOVER_REJECTED, id, pendingCount);
      throw new ShiftHandoverException(ErrorCodes.HANDOVER_ITEMS_PENDING, reason);
    }
    if (ShiftHandoverStatus.CLOSED.equals(handover.status)) {
      throw new ShiftHandoverException(ErrorCodes.HANDOVER_NOT_CLAIMABLE, "交接单已关闭，不能重复关闭。");
    }
    handover.status = ShiftHandoverStatus.CLOSED;
    handover.closed_at = LocalDateTime.now().format(CANONICAL);
    if (payload != null && payload.close_note() != null && !payload.close_note().isBlank()) {
      handover.remark = (handover.remark == null ? "" : handover.remark + "\n") + "【关闭说明】" + payload.close_note().trim();
    }
    repo.save(handover);
    audit.info("{} id={} closed_by={}", LogTemplates.SHIFT_HANDOVER_CLOSE, id, handover.claimed_by);
    return ShiftHandoverDtoFactory.handoverDto(handover);
  }

  /** 同一仓库重叠时段有效交接单判定：start < other.end && end > other.start（边界相接不算重叠）。 */
  private ShiftHandover findOverlappingActive(Long warehouseId, LocalDateTime start, LocalDateTime end, Long excludeId) {
    return repo.findAll().stream()
        .filter(h -> h.isActive())
        .filter(h -> warehouseId.equals(h.warehouse_id))
        .filter(h -> excludeId == null || !excludeId.equals(h.id))
        .filter(h -> start.isBefore(parseTime(h.shift_end_at, "shift_end_at"))
            && end.isAfter(parseTime(h.shift_start_at, "shift_start_at")))
        .min(Comparator.comparing(h -> h.id))
        .orElse(null);
  }

  private ShiftHandover requireHandover(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new ShiftHandoverException(ErrorCodes.HANDOVER_NOT_FOUND, ErrorMessages.HANDOVER_NOT_FOUND));
  }

  private void validateCreate(CreateShiftHandoverPayload payload) {
    if (payload == null) {
      throw new ShiftHandoverException(ErrorCodes.VALIDATION_FAILED, "请求体不能为空。");
    }
    if (payload.warehouse_id() == null) {
      throw new ShiftHandoverException(ErrorCodes.VALIDATION_FAILED, "请选择交接仓库。");
    }
    if (payload.shift_owner() == null || payload.shift_owner().isBlank()) {
      throw new ShiftHandoverException(ErrorCodes.VALIDATION_FAILED, "请选择当班负责人。");
    }
    if (payload.items() == null || payload.items().isEmpty()) {
      throw new ShiftHandoverException(ErrorCodes.VALIDATION_FAILED, "至少登记一条交接事项。");
    }
  }

  private void validateItem(CreateShiftHandoverPayload.HandoverItemPayload item) {
    if (item == null) {
      throw new ShiftHandoverException(ErrorCodes.VALIDATION_FAILED, "交接事项不能为空。");
    }
    if (item.category() == null || !HandoverItemCategory.ALL.contains(item.category())) {
      throw new ShiftHandoverException(ErrorCodes.VALIDATION_FAILED, "交接事项类别不合法。");
    }
    if (item.content() == null || item.content().isBlank()) {
      throw new ShiftHandoverException(ErrorCodes.VALIDATION_FAILED, "请填写交接事项内容。");
    }
    if (item.assignee() == null || item.assignee().isBlank()) {
      throw new ShiftHandoverException(ErrorCodes.VALIDATION_FAILED, "每条交接事项都要指定接手人。");
    }
  }

  /** 兼容 yyyy-MM-ddTHH:mm 和 yyyy-MM-ddTHH:mm:ss（datetime-local 控件可能省略秒）。 */
  private LocalDateTime parseTime(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new ShiftHandoverException(ErrorCodes.VALIDATION_FAILED, "请填写值守时段：" + field + "。");
    }
    String normalized = value.trim().replace(" ", "T");
    try {
      if (normalized.length() == 16) {
        return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
      }
      return LocalDateTime.parse(normalized);
    } catch (DateTimeParseException ex) {
      throw new ShiftHandoverException(ErrorCodes.VALIDATION_FAILED, "时间格式不正确：" + value + "。");
    }
  }
}
