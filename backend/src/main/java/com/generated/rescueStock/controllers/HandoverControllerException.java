package com.generated.rescueStock.controllers;

import java.util.Map;
import com.generated.rescueStock.services.HandoverBusinessException;

/** Controller 层对业务异常的二次包装，保留集中错误码与现场参数 */
public class HandoverControllerException extends RuntimeException {
  private final String code;
  private final Map<String, Object> meta;

  public HandoverControllerException(HandoverBusinessException cause) {
    super(cause.getMessage(), cause);
    this.code = cause.getCode();
    this.meta = cause.getMeta();
  }

  public String getCode() {
    return code;
  }

  public Map<String, Object> getMeta() {
    return meta;
  }
}
