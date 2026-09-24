/**
 * 值班交接单状态（与后端 constants/ShiftHandoverStatus.java 重复定义）。
 * PENDING_CLAIM / CLAIMED 属于“有效交接单”，参与同一仓库的时段重叠校验。
 */
export const ShiftHandoverStatus = ["PENDING_CLAIM", "CLAIMED", "CLOSED"] as const;
export type ShiftHandoverStatus = (typeof ShiftHandoverStatus)[number];

export const ACTIVE_HANDOVER_STATUSES: ShiftHandoverStatus[] = ["PENDING_CLAIM", "CLAIMED"];

export const ShiftHandoverStatusText: Record<ShiftHandoverStatus, string> = {
  PENDING_CLAIM: "待领取",
  CLAIMED: "处理中",
  CLOSED: "已关闭"
};

export const isActiveHandoverStatus = (status: string): boolean =>
  (ACTIVE_HANDOVER_STATUSES as readonly string[]).includes(status);
