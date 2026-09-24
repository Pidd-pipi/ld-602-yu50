import type { HandoverSheet, HandoverCreatePayload } from "../types/HandoverSheet";
import type { HandoverItem } from "../types/HandoverItem";
import {
  createHandoverItem,
  createHandoverSheet
} from "../constructors/ShiftHandoverConstructor";
import { HandoverApiError } from "../api/HandoverApiError";

/**
 * 本地持久化引擎：后端接口不可达时兜底，规则与后端 ShiftHandoverService 保持一致。
 * 同一仓库重叠时段只能有一张有效（非 CLOSED）交接单；逐项处理完毕才允许关闭。
 */
const STORAGE_KEY = "rescue-stock:shift-handover:v1";

const at = (base: Date, dayOffset: number, hour: number, minute = 0): string => {
  const date = new Date(base);
  date.setDate(date.getDate() + dayOffset);
  date.setHours(hour, minute, 0, 0);
  return date.toISOString();
};

const seed = (): HandoverSheet[] => {
  const now = new Date();
  return [
    // 1 号仓库：当前白班已领取、处理中，演示逐项处理与关闭
    createHandoverSheet(
      1,
      1,
      "周敏（交班）",
      at(now, 0, 8),
      at(now, 0, 20),
      [
        {
          id: 1,
          type: "INBOUND",
          content: "300 箱瓶装水到货待验收入库（采购单 PO-2317）",
          assignee: "李航",
          status: "PENDING",
          result_note: "",
          completed_at: null
        },
        {
          id: 2,
          type: "EXPIRE",
          content: "批次 BN-20260801 压缩饼干 30 天内到期，需优先安排调拨",
          assignee: "李航",
          status: "DONE",
          result_note: "已与鼓楼避难点确认接收，明早出库",
          completed_at: at(now, 0, 10, 15)
        },
        {
          id: 3,
          type: "EXCEPTION",
          content: "A 区货架 2 层急救包账实不符，盘亏 4 件，待复核",
          assignee: "王倩",
          status: "PENDING",
          result_note: "",
          completed_at: null
        }
      ]
    ),
    // 2 号仓库：昨晚夜班已关闭，演示历史回看；该时段不再参与冲突校验
    createHandoverSheet(
      2,
      2,
      "陈立（交班）",
      at(now, -1, 20),
      at(now, 0, 8),
      [
        {
          id: 1,
          type: "EXCEPTION",
          content: "冷藏柜温度短时告警，已恢复并复测",
          assignee: "赵磊",
          status: "DONE",
          result_note: "温度回归 4℃，记录已存档",
          completed_at: at(now, 0, 6, 40)
        }
      ]
    )
  ];
};

/** 补齐种子单的业务状态：1 号单已领取处理中，2 号单已关闭归档 */
const applySeedStates = (rows: HandoverSheet[]) => {
  const claimed = rows.find((row) => row.id === 1);
  if (claimed && claimed.status === "PENDING") {
    claimed.status = "CLAIMED";
    claimed.claimed_by = "李航";
    claimed.claimed_at = claimed.shift_start;
  }
  const closed = rows.find((row) => row.id === 2);
  if (closed && closed.status === "PENDING") {
    closed.status = "CLOSED";
    closed.claimed_by = "赵磊";
    closed.claimed_at = closed.shift_start;
    closed.closed_at = closed.items.find((item) => item.completed_at)?.completed_at ?? null;
  }
  return rows;
};

const load = (): HandoverSheet[] => {
  if (typeof localStorage === "undefined") return applySeedStates(seed());
  const raw = localStorage.getItem(STORAGE_KEY);
  if (raw) {
    try {
      return JSON.parse(raw) as HandoverSheet[];
    } catch {
      // 损坏数据回退到种子
    }
  }
  const rows = applySeedStates(seed());
  localStorage.setItem(STORAGE_KEY, JSON.stringify(rows));
  return rows;
};

const persist = (rows: HandoverSheet[]) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(rows));
};

const overlaps = (start: string, end: string, sheet: HandoverSheet) =>
  new Date(start) < new Date(sheet.shift_end) && new Date(sheet.shift_start) < new Date(end);

export const localHandoverEngine = {
  list(warehouseId?: number): HandoverSheet[] {
    const rows = load();
    return rows
      .filter((row) => warehouseId === undefined || row.warehouse_id === warehouseId)
      .sort((a, b) => (a.shift_start < b.shift_start ? 1 : -1));
  },

  create(payload: HandoverCreatePayload): HandoverSheet {
    const rows = load();
    const conflict = rows.find(
      (row) => row.warehouse_id === payload.warehouse_id && row.status !== "CLOSED" && overlaps(payload.shift_start, payload.shift_end, row)
    );
    if (conflict) {
      throw new HandoverApiError("HANDOVER_SHIFT_CONFLICT", "该仓库所选时段与有效交接单冲突", {
        conflict_id: conflict.id,
        conflict_status: conflict.status,
        conflict_start: conflict.shift_start,
        conflict_end: conflict.shift_end
      });
    }
    const id = rows.reduce((max, row) => Math.max(max, row.id), 0) + 1;
    const items: HandoverItem[] = payload.items.map((input, index) => createHandoverItem(index + 1, input));
    const sheet = createHandoverSheet(id, payload.warehouse_id, payload.manager_name, payload.shift_start, payload.shift_end, items);
    rows.push(sheet);
    persist(rows);
    return sheet;
  },

  claim(id: number, claimedBy: string): HandoverSheet {
    const rows = load();
    const sheet = rows.find((row) => row.id === id);
    if (!sheet) throw new HandoverApiError("HANDOVER_NOT_FOUND", "交接单不存在", { id });
    if (sheet.status !== "PENDING") {
      throw new HandoverApiError("HANDOVER_NOT_PENDING", "交接单已被领取或已关闭，不能重复领取", {
        id,
        status: sheet.status
      });
    }
    sheet.status = "CLAIMED";
    sheet.claimed_by = claimedBy;
    sheet.claimed_at = new Date().toISOString();
    persist(rows);
    return sheet;
  },

  completeItem(id: number, itemId: number, note: string): HandoverSheet {
    const rows = load();
    const sheet = rows.find((row) => row.id === id);
    if (!sheet) throw new HandoverApiError("HANDOVER_NOT_FOUND", "交接单不存在", { id });
    if (sheet.status !== "CLAIMED") {
      throw new HandoverApiError("HANDOVER_NOT_PENDING", "只有处理中的交接单可以登记事项结果", {
        id,
        status: sheet.status
      });
    }
    const item = sheet.items.find((entry) => entry.id === itemId);
    if (!item) throw new HandoverApiError("HANDOVER_NOT_FOUND", "交接事项不存在", { id, item_id: itemId });
    item.status = "DONE";
    item.result_note = note.trim();
    item.completed_at = new Date().toISOString();
    persist(rows);
    return sheet;
  },

  close(id: number): HandoverSheet {
    const rows = load();
    const sheet = rows.find((row) => row.id === id);
    if (!sheet) throw new HandoverApiError("HANDOVER_NOT_FOUND", "交接单不存在", { id });
    if (sheet.status !== "CLAIMED") {
      throw new HandoverApiError("HANDOVER_NOT_PENDING", "只有处理中的交接单可以关闭", {
        id,
        status: sheet.status
      });
    }
    const pendingCount = sheet.items.filter((item) => item.status !== "DONE").length;
    if (pendingCount > 0) {
      throw new HandoverApiError("HANDOVER_ITEM_PENDING", "仍有事项未处理，全部完成后才能关闭", {
        id,
        count: pendingCount
      });
    }
    sheet.status = "CLOSED";
    sheet.closed_at = new Date().toISOString();
    persist(rows);
    return sheet;
  }
};
