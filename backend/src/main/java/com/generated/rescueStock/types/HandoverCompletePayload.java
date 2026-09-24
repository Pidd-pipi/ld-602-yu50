package com.generated.rescueStock.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HandoverCompletePayload(@JsonProperty("result_note") String resultNote) {

  public String validate() {
    return resultNote == null || resultNote.isBlank() ? "请填写事项处理结果" : null;
  }
}
