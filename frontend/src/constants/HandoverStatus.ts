export const HandoverStatus = ["PENDING", "CLAIMED", "CLOSED"] as const;
export type HandoverStatus = (typeof HandoverStatus)[number];
export const HandoverStatusText: Record<HandoverStatus, string> = {
  PENDING: "待领取",
  CLAIMED: "处理中",
  CLOSED: "已关闭"
};
export const HandoverStatusTone: Record<HandoverStatus, "ok" | "warn" | "info"> = {
  PENDING: "warn",
  CLAIMED: "info",
  CLOSED: "ok"
};
