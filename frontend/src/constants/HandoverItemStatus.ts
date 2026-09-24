export const HandoverItemStatus = ["PENDING", "DONE"] as const;
export type HandoverItemStatus = (typeof HandoverItemStatus)[number];
export const HandoverItemStatusText: Record<HandoverItemStatus, string> = {
  PENDING: "待处理",
  DONE: "已处理"
};
