package com.generated.rescueStock.constants;

public final class LogTemplates {
  public static final String CREATE="create";
  public static final String UPDATE="update";
  public static final String STATUS="status";
  public static final String EXPORT="export";
  /** 值班交接：创建交接单 */
  public static final String SHIFT_HANDOVER_CREATE="值班交接单创建";
  /** 值班交接：新值班员领取 */
  public static final String SHIFT_HANDOVER_CLAIM="值班交接单领取";
  /** 值班交接：逐项处理事项 */
  public static final String SHIFT_HANDOVER_ITEM_PROCESS="值班交接事项处理";
  /** 值班交接：全部办结后关闭 */
  public static final String SHIFT_HANDOVER_CLOSE="值班交接单关闭";
  /** 值班交接：时段冲突或状态冲突被拒绝（不产生数据变更） */
  public static final String SHIFT_HANDOVER_REJECTED="值班交接操作被拒绝";
}
