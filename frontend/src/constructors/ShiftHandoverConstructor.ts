import type { HandoverItem } from "../types/HandoverItem";
import type { HandoverSheet } from "../types/HandoverSheet";
import type { HandoverItemInput } from "../types/HandoverItem";
import type { HandoverItemType } from "../constants/HandoverItemType";

const pad = (value: number) => String(value).padStart(2, "0");

/** 本地 datetime-local 控件使用的 yyyy-MM-ddTHH:mm 字符串 */
export const toLocalInputValue = (date: Date): string =>
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;

/** datetime-local 值转 ISO 字符串（作为接口与存储格式） */
export const toIso = (localInput: string): string => new Date(localInput).toISOString();

export const createDefaultHandoverItemInput = (overrides: Partial<HandoverItemInput> = {}): HandoverItemInput => ({
  type: "INBOUND",
  content: "",
  assignee: "",
  ...overrides
});

export const createDefaultHandoverForm = (warehouseId: number) => {
  const start = new Date();
  start.setHours(20, 0, 0, 0);
  const end = new Date(start.getTime() + 12 * 60 * 60 * 1000);
  return {
    warehouse_id: warehouseId,
    manager_name: "",
    shift_start: toLocalInputValue(start),
    shift_end: toLocalInputValue(end),
    items: [createDefaultHandoverItemInput()]
  };
};

export const createHandoverItem = (id: number, input: HandoverItemInput): HandoverItem => ({
  id,
  type: input.type,
  content: input.content.trim(),
  assignee: input.assignee.trim(),
  status: "PENDING",
  result_note: "",
  completed_at: null
});

export const createHandoverSheet = (
  id: number,
  warehouseId: number,
  managerName: string,
  shiftStart: string,
  shiftEnd: string,
  items: HandoverItem[]
): HandoverSheet => ({
  id,
  warehouse_id: warehouseId,
  manager_name: managerName.trim(),
  shift_start: shiftStart,
  shift_end: shiftEnd,
  status: "PENDING",
  claimed_by: null,
  claimed_at: null,
  created_at: new Date().toISOString(),
  closed_at: null,
  items
});

export const createHandoverItemTypeOptions = (): Array<{ value: HandoverItemType; label: string }> => [
  { value: "INBOUND", label: "待入库" },
  { value: "EXPIRE", label: "临期批次" },
  { value: "EXCEPTION", label: "异常库存" },
  { value: "OTHER", label: "其他事项" }
];
