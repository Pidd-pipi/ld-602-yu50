package com.generated.rescueStock.constants;

public final class ErrorMessages {
  public static final String AUTH_REQUIRED="missing token";
  public static final String RBAC_DENIED="role denied";
  public static final String VALIDATION_FAILED="表单字段缺失或格式错误";
  public static final String RATE_LIMITED="请求过于频繁，请稍后再试";
  public static final String HANDOVER_SHIFT_CONFLICT="该仓库在所选时段内已有一张有效交接单（%s），同一仓库的重叠时段只能有一张有效交接单，原单与库存保持不变。";
  public static final String HANDOVER_NOT_CLAIMABLE="交接单已被 %s 领取（当前状态：%s），不能重复领取，原单与库存保持不变。";
  public static final String HANDOVER_ITEMS_PENDING="仍有 %d 条交接事项未处理，全部事项处理完成后才能关闭交接单。";
  public static final String HANDOVER_NOT_FOUND="交接单不存在或已被删除。";
  public static final String HANDOVER_ITEM_NOT_FOUND="交接事项不存在或不属于该交接单。";
}
