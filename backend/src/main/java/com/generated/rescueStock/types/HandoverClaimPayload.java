package com.generated.rescueStock.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HandoverClaimPayload(@JsonProperty("claimed_by") String claimedBy) {

  public String validate() {
    return claimedBy == null || claimedBy.isBlank() ? "请填写领取人姓名" : null;
  }
}
