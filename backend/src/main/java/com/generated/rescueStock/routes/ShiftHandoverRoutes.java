package com.generated.rescueStock.routes;

/** 值班交接路由常量，控制器路径与前端 /api/shift-handover 保持一致。 */
public final class ShiftHandoverRoutes {
  private ShiftHandoverRoutes() {}

  public static final String PATH = "/api/shift-handover";
  public static final String BY_ID = "/api/shift-handover/{id}";
  public static final String CLAIM = "/api/shift-handover/{id}/claim";
  public static final String ITEM = "/api/shift-handover/{id}/items/{itemId}";
  public static final String CLOSE = "/api/shift-handover/{id}/close";
}
