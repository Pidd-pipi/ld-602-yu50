package com.generated.rescueStock.repositories;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;
import com.generated.rescueStock.constants.HandoverItemCategory;
import com.generated.rescueStock.constants.HandoverItemStatus;
import com.generated.rescueStock.constants.ShiftHandoverStatus;
import com.generated.rescueStock.models.HandoverItem;
import com.generated.rescueStock.models.ShiftHandover;

/**
 * 值班交接单数据访问层。
 * 说明：项目暂未接入真实数据源，此处以线程安全的内存表承载交接单状态，
 * 表结构与 database/init.sql 中的 shift_handover / handover_item 对齐。
 */
@Repository
public class ShiftHandoverRepository {
  private final ConcurrentHashMap<Long, ShiftHandover> table = new ConcurrentHashMap<>();
  private final AtomicLong handoverSequence = new AtomicLong(100);
  private final AtomicLong itemSequence = new AtomicLong(100);

  public ShiftHandoverRepository() {
    seed();
  }

  public List<ShiftHandover> findAll() {
    return table.values().stream()
        .sorted(Comparator.comparing((ShiftHandover h) -> h.shift_start_at).reversed())
        .toList();
  }

  public Optional<ShiftHandover> findById(Long id) {
    return Optional.ofNullable(table.get(id));
  }

  public ShiftHandover save(ShiftHandover handover) {
    if (handover.id == null) {
      handover.id = handoverSequence.incrementAndGet();
    }
    table.put(handover.id, handover);
    return handover;
  }

  public Long nextItemId() {
    return itemSequence.incrementAndGet();
  }

  private void seed() {
    // 1 号仓：一张待领取的交接单（种子基准日 2026-09-24）
    ShiftHandover pending = new ShiftHandover();
    pending.id = 1L;
    pending.warehouse_id = 1L;
    pending.shift_owner = "周敏（白班）";
    pending.claimed_by = null;
    pending.shift_start_at = "2026-09-24T08:00:00";
    pending.shift_end_at = "2026-09-24T20:00:00";
    pending.remark = "夜班重点关注临期饮用水和 3 号道口待入库物资。";
    pending.status = ShiftHandoverStatus.PENDING_CLAIM;
    pending.created_at = "2026-09-24T07:50:00";
    pending.items.add(item(10L, pending.id, HandoverItemCategory.INBOUND_PENDING,
        "3 号道口待入库：矿泉水 200 箱（随车单 IN-20260924-03），等待抽检登记。", "李建国", "B-待入-003", 1));
    pending.items.add(item(11L, pending.id, HandoverItemCategory.EXPIRING_BATCH,
        "批次 W-B20260901 瓶装水 30 天内到期，优先安排调拨或复检。", "王海涛", "W-B20260901", 2));
    pending.items.add(item(12L, pending.id, HandoverItemCategory.ABNORMAL_INVENTORY,
        "应急灯 BX-07 账实不符：账面 40，实盘 36，需复核领用记录。", "赵倩", "BX-07", 3));
    table.put(pending.id, pending);

    // 2 号仓：一张已领取、事项处理中的交接单
    ShiftHandover claimed = new ShiftHandover();
    claimed.id = 2L;
    claimed.warehouse_id = 2L;
    claimed.shift_owner = "孙立军（夜班）";
    claimed.claimed_by = "陈晨";
    claimed.shift_start_at = "2026-09-24T00:00:00";
    claimed.shift_end_at = "2026-09-24T12:00:00";
    claimed.remark = "药品库温湿度记录需补齐。";
    claimed.status = ShiftHandoverStatus.CLAIMED;
    claimed.created_at = "2026-09-23T23:40:00";
    claimed.claimed_at = "2026-09-24T00:05:00";
    HandoverItem done = item(20L, claimed.id, HandoverItemCategory.EXPIRING_BATCH,
        "医用口罩批次 M-M20260815 临期，已移入临期货架。", "陈晨", "M-M20260815", 1);
    done.status = HandoverItemStatus.DONE;
    done.processed_at = "2026-09-24T02:10:00";
    done.process_note = "已完成移库并贴临期标签。";
    claimed.items.add(done);
    claimed.items.add(item(21L, claimed.id, HandoverItemCategory.ABNORMAL_INVENTORY,
        "消毒液台账缺 9 月 22 日领用签字，需找领用班组补签。", "林一帆", "D-台账", 2));
    table.put(claimed.id, claimed);

    // 1 号仓：一张历史已关闭交接单，用于按仓库回看
    ShiftHandover closed = new ShiftHandover();
    closed.id = 3L;
    closed.warehouse_id = 1L;
    closed.shift_owner = "周敏（白班）";
    closed.claimed_by = "王海涛";
    closed.shift_start_at = "2026-09-23T08:00:00";
    closed.shift_end_at = "2026-09-23T20:00:00";
    closed.remark = "台风前置备货日。";
    closed.status = ShiftHandoverStatus.CLOSED;
    closed.created_at = "2026-09-23T07:45:00";
    closed.claimed_at = "2026-09-23T08:02:00";
    closed.closed_at = "2026-09-23T20:30:00";
    HandoverItem c1 = item(30L, closed.id, HandoverItemCategory.INBOUND_PENDING,
        "方便食品 120 箱入库登记。", "王海涛", "F-B20260923", 1);
    c1.status = HandoverItemStatus.DONE;
    c1.processed_at = "2026-09-23T10:20:00";
    c1.process_note = "入库完成，账实一致。";
    closed.items.add(c1);
    table.put(closed.id, closed);
  }

  private HandoverItem item(Long id, Long handoverId, String category, String content, String assignee, String batchNo, int sortNo) {
    HandoverItem item = new HandoverItem();
    item.id = id;
    item.handover_id = handoverId;
    item.category = category;
    item.content = content;
    item.assignee = assignee;
    item.batch_no = batchNo;
    item.status = HandoverItemStatus.PENDING;
    item.sort_no = sortNo;
    return item;
  }
}
