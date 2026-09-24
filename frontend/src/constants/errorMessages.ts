export const ERROR_MESSAGES = {
  AUTH_REQUIRED: "请先登录后再继续操作",
  RBAC_DENIED: "当前角色没有执行该动作的权限",
  VALIDATION_FAILED: "表单字段缺失或格式错误",
  RATE_LIMITED: "请求过于频繁，请稍后再试",
  HANDOVER_SHIFT_CONFLICT: "该仓库所选时段已有有效交接单（{code}，{window}），同一仓库重叠时段只能有一张有效交接单",
  HANDOVER_NOT_PENDING: "交接单 {code} 当前为{status}状态，仅待领取交接单可被领取",
  HANDOVER_ITEM_PENDING: "交接单 {code} 仍有 {count} 项事项未处理，全部完成后才能关闭",
  HANDOVER_NOT_FOUND: "交接单 {code} 不存在或已失效"
};
