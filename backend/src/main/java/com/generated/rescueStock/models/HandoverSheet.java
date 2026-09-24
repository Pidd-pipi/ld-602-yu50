package com.generated.rescueStock.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class HandoverSheet {
  public Long id;
  public Long warehouseId;
  public String managerName;
  public Instant shiftStart;
  public Instant shiftEnd;
  public String status;
  public String claimedBy;
  public Instant claimedAt;
  public Instant createdAt;
  public Instant closedAt;
  public List<HandoverItem> items = new ArrayList<>();
}
