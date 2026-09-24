package com.generated.rescueStock.services;

import java.util.Map;

public class HandoverBusinessException extends RuntimeException {
  private final String code;
  private final Map<String, Object> meta;

  public HandoverBusinessException(String code, String message, Map<String, Object> meta) {
    super(message);
    this.code = code;
    this.meta = meta;
  }

  public String getCode() {
    return code;
  }

  public Map<String, Object> getMeta() {
    return meta;
  }
}
