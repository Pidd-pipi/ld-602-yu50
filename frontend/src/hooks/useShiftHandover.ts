import type { HandoverSheet } from "../types/HandoverSheet";
import { HandoverStatusText } from "../constants/HandoverStatus";
import { HandoverItemTypeText } from "../constants/HandoverItemType";
import { formatHandoverCode, formatShiftWindow, formatDateTimeShort } from "../utils/formatters";

/** 值班交接领域逻辑：进度统计、时段重叠预检、文案格式化 */
export function useShiftHandover() {
  const progressOf = (sheet: HandoverSheet) => {
    const total = sheet.items.length;
    const done = sheet.items.filter((item) => item.status === "DONE").length;
    return {
      total,
      done,
      pending: total - done,
      percent: total === 0 ? 0 : Math.round((done / total) * 100),
      allDone: total > 0 && done === total
    };
  };

  /** 同仓库有效（非 CLOSED）交接单的半开区间重叠预检 */
  const findOverlap = (rows: HandoverSheet[], warehouseId: number, start: string, end: string, excludeId?: number) =>
    rows.find(
      (row) =>
        row.warehouse_id === warehouseId &&
        row.id !== excludeId &&
        row.status !== "CLOSED" &&
        new Date(start) < new Date(row.shift_end) &&
        new Date(row.shift_start) < new Date(end)
    );

  const statusLabel = (status: HandoverSheet["status"]) => HandoverStatusText[status];
  const typeLabel = (type: keyof typeof HandoverItemTypeText) => HandoverItemTypeText[type];
  const codeOf = (id: number) => formatHandoverCode(id);
  const windowOf = (sheet: HandoverSheet) => formatShiftWindow(sheet.shift_start, sheet.shift_end);
  const timeOf = (value: string | null) => (value ? formatDateTimeShort(value) : "—");

  const validateWindow = (start: string, end: string) => {
    if (!start || !end) return "请选择交接时段的开始与结束时间";
    if (new Date(end) <= new Date(start)) return "时段结束时间必须晚于开始时间";
    return null;
  };

  return {
    progressOf,
    findOverlap,
    validateWindow,
    statusLabel,
    typeLabel,
    codeOf,
    windowOf,
    timeOf,
    canClaim: (sheet: HandoverSheet) => sheet.status === "PENDING",
    canClose: (sheet: HandoverSheet) => progressOf(sheet).allDone && sheet.status === "CLAIMED"
  };
}
