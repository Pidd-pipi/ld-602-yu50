package com.generated.rescueStock.models;

import java.time.Instant;

public class HandoverItem {
  public Long id;
  public String type;
  public String content;
  public String assignee;
  public String status;
  public String resultNote;
  public Instant completedAt;

  public HandoverItem() {}

  public HandoverItem(Long id, String type, String content, String assignee, String status) {
    this.id = id;
    this.type = type;
    this.content = content;
    this.assignee = assignee;
    this.status = status;
  }
}
