/** 交接事项处理状态（与后端 constants/HandoverItemStatus.java 重复定义）。全部 DONE 后才能关闭交接单。 */
export const HandoverItemStatus = ["PENDING", "DONE"] as const;
export type HandoverItemStatus = (typeof HandoverItemStatus)[number];

export const HandoverItemStatusText: Record<HandoverItemStatus, string> = {
  PENDING: "待处理",
  DONE: "已处理"
};
