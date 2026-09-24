/** 交接事项类别（与后端 constants/HandoverItemCategory.java 重复定义）。 */
export const HandoverItemCategory = ["INBOUND_PENDING", "EXPIRING_BATCH", "ABNORMAL_INVENTORY", "OTHER"] as const;
export type HandoverItemCategory = (typeof HandoverItemCategory)[number];

export const HandoverItemCategoryText: Record<HandoverItemCategory, string> = {
  INBOUND_PENDING: "待入库",
  EXPIRING_BATCH: "临期批次",
  ABNORMAL_INVENTORY: "异常库存",
  OTHER: "其他事项"
};
