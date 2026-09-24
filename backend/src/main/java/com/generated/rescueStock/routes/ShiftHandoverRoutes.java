package com.generated.rescueStock.routes;

public final class ShiftHandoverRoutes {
  public static final String PATH = "/api/shift-handover";
  public static final String CLAIM = PATH + "/{id}/claim";
  public static final String ITEM_COMPLETE = PATH + "/{id}/items/{itemId}/complete";
  public static final String CLOSE = PATH + "/{id}/close";

  private ShiftHandoverRoutes() {}
}
