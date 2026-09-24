package com.generated.rescueStock.services;

import com.generated.rescueStock.constants.ErrorCodes;

/** 值班交接业务异常：携带错误码，由控制器包装成带原因提示的 4xx 响应，且不触发任何数据写入。 */
public class ShiftHandoverException extends RuntimeException {
  private final String code;

  public ShiftHandoverException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static ShiftHandoverException conflict(String message) {
    return new ShiftHandoverException(ErrorCodes.HANDOVER_SHIFT_CONFLICT, message);
  }

  public static ShiftHandoverException notClaimable(String message) {
    return new ShiftHandoverException(ErrorCodes.HANDOVER_NOT_CLAIMABLE, message);
  }
}
