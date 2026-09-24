export const HandoverItemType = ["INBOUND", "EXPIRE", "EXCEPTION", "OTHER"] as const;
export type HandoverItemType = (typeof HandoverItemType)[number];
export const HandoverItemTypeText: Record<HandoverItemType, string> = {
  INBOUND: "待入库",
  EXPIRE: "临期批次",
  EXCEPTION: "异常库存",
  OTHER: "其他事项"
};
