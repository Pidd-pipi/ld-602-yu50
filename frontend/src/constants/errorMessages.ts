export const ERROR_MESSAGES = {
  AUTH_REQUIRED: "请先登录后再继续操作",
  RBAC_DENIED: "当前角色没有执行该动作的权限",
  VALIDATION_FAILED: "表单字段缺失或格式错误",
  RATE_LIMITED: "请求过于频繁，请稍后再试",
  HANDOVER_SHIFT_CONFLICT: "该仓库在所选时段内已有一张有效交接单，同一仓库的重叠时段只能有一张有效交接单，原单与库存保持不变。",
  HANDOVER_NOT_CLAIMABLE: "交接单已被其他值班员领取或已关闭，不能重复领取，原单与库存保持不变。",
  HANDOVER_ITEMS_PENDING: "仍有交接事项未处理，全部事项处理完成后才能关闭交接单。",
  HANDOVER_NOT_FOUND: "交接单不存在或已被删除。",
  HANDOVER_ITEM_NOT_FOUND: "交接事项不存在或不属于该交接单。"
};
