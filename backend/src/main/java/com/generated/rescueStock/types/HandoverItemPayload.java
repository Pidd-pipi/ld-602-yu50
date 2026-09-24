package com.generated.rescueStock.types;

import java.util.List;

public record HandoverItemPayload(String type, String content, String assignee) {

  public List<String> validate() {
    return java.util.stream.Stream.of(
        type == null || type.isBlank() ? "事项类型不能为空" : "",
        content == null || content.isBlank() ? "事项内容不能为空" : "",
        assignee == null || assignee.isBlank() ? "接手人不能为空" : "")
        .filter(message -> !message.isEmpty())
        .toList();
  }
}
