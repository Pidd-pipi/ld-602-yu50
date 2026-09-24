package com.generated.rescueStock.constants;

public final class ErrorCodes {
  public static final String AUTH_REQUIRED="AUTH_REQUIRED";
  public static final String RBAC_DENIED="RBAC_DENIED";
  public static final String VALIDATION_FAILED="VALIDATION_FAILED";
  public static final String RATE_LIMITED="RATE_LIMITED";
  /** 同一仓库重叠时段已存在有效交接单。 */
  public static final String HANDOVER_SHIFT_CONFLICT="HANDOVER_SHIFT_CONFLICT";
  /** 交接单已被领取或已关闭，不能再次领取。 */
  public static final String HANDOVER_NOT_CLAIMABLE="HANDOVER_NOT_CLAIMABLE";
  /** 仍有交接事项未处理完，不能关闭交接单。 */
  public static final String HANDOVER_ITEMS_PENDING="HANDOVER_ITEMS_PENDING";
  /** 交接单不存在。 */
  public static final String HANDOVER_NOT_FOUND="HANDOVER_NOT_FOUND";
  /** 交接事项不存在或不属于该交接单。 */
  public static final String HANDOVER_ITEM_NOT_FOUND="HANDOVER_ITEM_NOT_FOUND";
}
