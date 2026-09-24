package com.generated.rescueStock.constants;

public final class ErrorMessages {
  public static final String AUTH_REQUIRED = "missing token";
  public static final String RBAC_DENIED = "role denied";
  public static final String HANDOVER_SHIFT_CONFLICT = "同仓库所选时段与有效交接单冲突";
  public static final String HANDOVER_NOT_PENDING = "交接单当前状态不允许该操作";
  public static final String HANDOVER_ITEM_PENDING = "仍有交接事项未处理，全部完成后才能关闭";
  public static final String HANDOVER_NOT_FOUND = "交接单或事项不存在";

  private ErrorMessages() {}
}
