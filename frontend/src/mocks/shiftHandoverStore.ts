import type { CreateShiftHandoverInput, HandoverItem, ShiftHandover } from "../types/ShiftHandover";

/**
 * 值班交接本地兜底存储（后端不可达时使用，与后端 ShiftHandoverRepository 种子一致）。
 * 规则与后端 ShiftHandoverService 保持一致：同仓重叠时段唯一有效单、领取后逐项处理、全部办结才能关闭。
 * 使用 localStorage，重开页面后仍可按仓库回看交接内容。
 */

const STORAGE_KEY = "rescue-stock.shiftHandover.v1";
const OPERATOR_KEY = "rescue-stock.lastOperator.v1";

export interface HandoverRuleError {
  code: string;
  message: string;
}

export function getLastOperator(): string {
  try {
    return localStorage.getItem(OPERATOR_KEY) ?? "";
  } catch {
    return "";
  }
}

export function saveLastOperator(name: string): void {
  try {
    if (name) localStorage.setItem(OPERATOR_KEY, name);
  } catch {
    // 隐私模式下忽略
  }
}

const seedRows = (): ShiftHandover[] => [
  {
    id: 1,
    warehouse_id: 1,
    shift_owner: "周敏（白班）",
    claimed_by: null,
    shift_start_at: "2026-09-24T08:00",
    shift_end_at: "2026-09-24T20:00",
    remark: "夜班重点关注临期饮用水和 3 号道口待入库物资。",
    status: "PENDING_CLAIM",
    created_at: "2026-09-24T07:50",
    claimed_at: null,
    closed_at: null,
    items: [
      { id: 10, handover_id: 1, category: "INBOUND_PENDING", content: "3 号道口待入库：矿泉水 200 箱（随车单 IN-20260924-03），等待抽检登记。", assignee: "李建国", batch_no: "B-待入-003", status: "PENDING", sort_no: 1 },
      { id: 11, handover_id: 1, category: "EXPIRING_BATCH", content: "批次 W-B20260901 瓶装水 30 天内到期，优先安排调拨或复检。", assignee: "王海涛", batch_no: "W-B20260901", status: "PENDING", sort_no: 2 },
      { id: 12, handover_id: 1, category: "ABNORMAL_INVENTORY", content: "应急灯 BX-07 账实不符：账面 40，实盘 36，需复核领用记录。", assignee: "赵倩", batch_no: "BX-07", status: "PENDING", sort_no: 3 }
    ]
  },
  {
    id: 2,
    warehouse_id: 2,
    shift_owner: "孙立军（夜班）",
    claimed_by: "陈晨",
    shift_start_at: "2026-09-24T00:00",
    shift_end_at: "2026-09-24T12:00",
    remark: "药品库温湿度记录需补齐。",
    status: "CLAIMED",
    created_at: "2026-09-23T23:40",
    claimed_at: "2026-09-24T00:05",
    closed_at: null,
    items: [
      { id: 20, handover_id: 2, category: "EXPIRING_BATCH", content: "医用口罩批次 M-M20260815 临期，已移入临期货架。", assignee: "陈晨", batch_no: "M-M20260815", status: "DONE", processed_at: "2026-09-24T02:10", process_note: "已完成移库并贴临期标签。", sort_no: 1 },
      { id: 21, handover_id: 2, category: "ABNORMAL_INVENTORY", content: "消毒液台账缺 9 月 22 日领用签字，需找领用班组补签。", assignee: "林一帆", batch_no: "D-台账", status: "PENDING", sort_no: 2 }
    ]
  },
  {
    id: 3,
    warehouse_id: 1,
    shift_owner: "周敏（白班）",
    claimed_by: "王海涛",
    shift_start_at: "2026-09-23T08:00",
    shift_end_at: "2026-09-23T20:00",
    remark: "台风前置备货日。",
    status: "CLOSED",
    created_at: "2026-09-23T07:45",
    claimed_at: "2026-09-23T08:02",
    closed_at: "2026-09-23T20:30",
    items: [
      { id: 30, handover_id: 3, category: "INBOUND_PENDING", content: "方便食品 120 箱入库登记。", assignee: "王海涛", batch_no: "F-B20260923", status: "DONE", processed_at: "2026-09-23T10:20", process_note: "入库完成，账实一致。", sort_no: 1 }
    ]
  }
];

const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value)) as T;

function load(): ShiftHandover[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as ShiftHandover[];
      if (Array.isArray(parsed)) return parsed;
    }
  } catch {
    // 数据损坏时回退到种子
  }
  const seed = seedRows();
  persist(seed);
  return seed;
}

function persist(rows: ShiftHandover[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(rows));
  } catch {
    // 容量或隐私模式问题忽略
  }
}

const fail = (code: string, message: string): HandoverRuleError => ({ code, message });

const parseTime = (value: string): number => {
  const normalized = value.trim().replace(" ", "T");
  const time = new Date(normalized.length === 16 ? normalized + ":00" : normalized).getTime();
  if (Number.isNaN(time)) throw fail("VALIDATION_FAILED", `时间格式不正确：${value}。`);
  return time;
};

const nowText = (): string => {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

export function localListShiftHandover(warehouseId?: number | null): ShiftHandover[] {
  const rows = load().slice().sort((a, b) => b.shift_start_at.localeCompare(a.shift_start_at));
  return clone(warehouseId ? rows.filter((row) => row.warehouse_id === warehouseId) : rows);
}

export function localCreateShiftHandover(input: CreateShiftHandoverInput): ShiftHandover | HandoverRuleError {
  if (!input.warehouse_id) return fail("VALIDATION_FAILED", "请选择交接仓库。");
  if (!input.shift_owner?.trim()) return fail("VALIDATION_FAILED", "请选择当班负责人。");
  if (!input.shift_start_at || !input.shift_end_at) return fail("VALIDATION_FAILED", "请选择完整的值守时段。");
  if (!input.items?.length) return fail("VALIDATION_FAILED", "至少登记一条交接事项。");
  const start = parseTime(input.shift_start_at);
  const end = parseTime(input.shift_end_at);
  if (!(end > start)) return fail("VALIDATION_FAILED", "值守结束时间必须晚于开始时间。");
  for (const item of input.items) {
    if (!item.content?.trim()) return fail("VALIDATION_FAILED", "请填写每条交接事项的内容。");
    if (!item.assignee?.trim()) return fail("VALIDATION_FAILED", "每条交接事项都要指定接手人。");
  }

  const rows = load();
  const conflict = rows
    .filter((row) => row.warehouse_id === input.warehouse_id && row.status !== "CLOSED")
    .find((row) => {
      const otherStart = parseTime(row.shift_start_at);
      const otherEnd = parseTime(row.shift_end_at);
      return start < otherEnd && end > otherStart;
    });
  if (conflict) {
    return fail(
      "HANDOVER_SHIFT_CONFLICT",
      `该仓库在所选时段内已有一张有效交接单（#${conflict.id}，状态 ${statusText(conflict.status)}，${conflict.shift_start_at} ~ ${conflict.shift_end_at}）。同一仓库的重叠时段只能有一张有效交接单，原单与库存保持不变。`
    );
  }

  const nextId = rows.reduce((max, row) => Math.max(max, row.id), 0) + 1;
  let itemSeq = rows.flatMap((row) => row.items).reduce((max, item) => Math.max(max, item.id), 0);
  const handover: ShiftHandover = {
    id: nextId,
    warehouse_id: input.warehouse_id,
    shift_owner: input.shift_owner.trim(),
    claimed_by: null,
    shift_start_at: input.shift_start_at,
    shift_end_at: input.shift_end_at,
    remark: input.remark ?? "",
    status: "PENDING_CLAIM",
    created_at: nowText(),
    claimed_at: null,
    closed_at: null,
    items: input.items.map((item, index): HandoverItem => ({
      id: ++itemSeq,
      handover_id: nextId,
      category: item.category,
      content: item.content.trim(),
      assignee: item.assignee.trim(),
      batch_no: item.batch_no ?? "",
      status: "PENDING",
      sort_no: index + 1
    }))
  };
  rows.push(handover);
  persist(rows);
  return clone(handover);
}

export function localClaimShiftHandover(id: number, claimedBy: string): ShiftHandover | HandoverRuleError {
  if (!claimedBy?.trim()) return fail("VALIDATION_FAILED", "请填写领取交接单的值班员姓名。");
  const rows = load();
  const handover = rows.find((row) => row.id === id);
  if (!handover) return fail("HANDOVER_NOT_FOUND", "交接单不存在或已被删除。");
  if (handover.status !== "PENDING_CLAIM") {
    return fail(
      "HANDOVER_NOT_CLAIMABLE",
      `交接单已被 ${handover.claimed_by || "其他值班员"} 领取（当前状态：${statusText(handover.status)}），不能重复领取，原单与库存保持不变。`
    );
  }
  handover.claimed_by = claimedBy.trim();
  handover.status = "CLAIMED";
  handover.claimed_at = nowText();
  persist(rows);
  return clone(handover);
}

export function localProcessHandoverItem(
  id: number,
  itemId: number,
  status: string,
  processNote?: string
): ShiftHandover | HandoverRuleError {
  const rows = load();
  const handover = rows.find((row) => row.id === id);
  if (!handover) return fail("HANDOVER_NOT_FOUND", "交接单不存在或已被删除。");
  if (handover.status !== "CLAIMED") {
    return fail("HANDOVER_NOT_CLAIMABLE", `交接单当前状态为「${statusText(handover.status)}」，领取后才能逐项处理事项。`);
  }
  const item = handover.items.find((candidate) => candidate.id === itemId);
  if (!item) return fail("HANDOVER_ITEM_NOT_FOUND", "交接事项不存在或不属于该交接单。");
  item.status = status === "PENDING" ? "PENDING" : "DONE";
  if (item.status === "DONE") {
    item.processed_at = nowText();
    item.process_note = processNote ?? "";
  } else {
    item.processed_at = null;
    item.process_note = null;
  }
  persist(rows);
  return clone(handover);
}

export function localCloseShiftHandover(id: number, closeNote?: string): ShiftHandover | HandoverRuleError {
  const rows = load();
  const handover = rows.find((row) => row.id === id);
  if (!handover) return fail("HANDOVER_NOT_FOUND", "交接单不存在或已被删除。");
  if (handover.status === "CLOSED") return fail("HANDOVER_NOT_CLAIMABLE", "交接单已关闭，不能重复关闭。");
  const pending = handover.items.filter((item) => item.status !== "DONE").length;
  if (pending > 0) {
    return fail("HANDOVER_ITEMS_PENDING", `仍有 ${pending} 条交接事项未处理，全部事项处理完成后才能关闭交接单。`);
  }
  handover.status = "CLOSED";
  handover.closed_at = nowText();
  if (closeNote?.trim()) handover.remark = `${handover.remark ?? ""}\n【关闭说明】${closeNote.trim()}`;
  persist(rows);
  return clone(handover);
}

function statusText(status: string): string {
  return { PENDING_CLAIM: "待领取", CLAIMED: "处理中", CLOSED: "已关闭" }[status] ?? status;
}

export const isHandoverRuleError = (value: unknown): value is HandoverRuleError =>
  typeof value === "object" && value !== null && "code" in value && "message" in value;
