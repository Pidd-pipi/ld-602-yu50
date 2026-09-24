package com.generated.rescueStock.repositories;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;
import com.generated.rescueStock.constants.HandoverItemStatus;
import com.generated.rescueStock.constants.HandoverStatus;
import com.generated.rescueStock.models.HandoverItem;
import com.generated.rescueStock.models.HandoverSheet;

/**
 * 值班交接单仓储。骨架项目尚未接入数据源，先用进程内存储保持行为完整，
 * 后续接入 MyBatis-Plus 时仅替换本类，服务层规则不变。
 */
@Repository
public class ShiftHandoverRepository {

  private final List<HandoverSheet> rows = new CopyOnWriteArrayList<>();
  private long sheetSequence = 0L;

  public ShiftHandoverRepository() {
    seed();
  }

  public List<HandoverSheet> findAll() {
    List<HandoverSheet> copy = new ArrayList<>(rows);
    copy.sort(Comparator.comparing((HandoverSheet sheet) -> sheet.shiftStart).reversed());
    return copy;
  }

  public Optional<HandoverSheet> findById(Long id) {
    return rows.stream().filter(sheet -> sheet.id.equals(id)).findFirst();
  }

  public HandoverSheet save(HandoverSheet sheet) {
    if (sheet.id == null) {
      sheet.id = ++sheetSequence;
      rows.add(sheet);
    }
    return sheet;
  }

  private void seed() {
    Instant now = Instant.now().truncatedTo(ChronoUnit.HOURS);

    HandoverSheet claimed = new HandoverSheet();
    claimed.warehouseId = 1L;
    claimed.managerName = "周敏（交班）";
    claimed.shiftStart = now;
    claimed.shiftEnd = now.plus(12, ChronoUnit.HOURS);
    claimed.status = HandoverStatus.CLAIMED;
    claimed.claimedBy = "李航";
    claimed.claimedAt = now.plus(5, ChronoUnit.MINUTES);
    claimed.createdAt = now.minus(10, ChronoUnit.MINUTES);
    claimed.items.add(item(1L, "INBOUND", "300 箱瓶装水到货待验收入库（采购单 PO-2317）", "李航", HandoverItemStatus.PENDING, null, null));
    claimed.items.add(item(2L, "EXPIRE", "批次 BN-20260801 压缩饼干 30 天内到期，需优先安排调拨", "李航", HandoverItemStatus.DONE, "已与鼓楼避难点确认接收，明早出库", now.plus(2, ChronoUnit.HOURS)));
    claimed.items.add(item(3L, "EXCEPTION", "A 区货架 2 层急救包账实不符，盘亏 4 件，待复核", "王倩", HandoverItemStatus.PENDING, null, null));
    save(claimed);

    HandoverSheet closed = new HandoverSheet();
    closed.warehouseId = 2L;
    closed.managerName = "陈立（交班）";
    closed.shiftStart = now.minus(12, ChronoUnit.HOURS);
    closed.shiftEnd = now;
    closed.status = HandoverStatus.CLOSED;
    closed.claimedBy = "赵磊";
    closed.claimedAt = now.minus(11, ChronoUnit.HOURS);
    closed.createdAt = now.minus(13, ChronoUnit.HOURS);
    closed.closedAt = now.minus(20, ChronoUnit.MINUTES);
    closed.items.add(item(1L, "EXCEPTION", "冷藏柜温度短时告警，已恢复并复测", "赵磊", HandoverItemStatus.DONE, "温度回归 4℃，记录已存档", now.minus(90, ChronoUnit.MINUTES)));
    save(closed);
  }

  private HandoverItem item(Long id, String type, String content, String assignee, String status, String resultNote, Instant completedAt) {
    HandoverItem item = new HandoverItem(id, type, content, assignee, status);
    item.resultNote = resultNote;
    item.completedAt = completedAt;
    return item;
  }
}
