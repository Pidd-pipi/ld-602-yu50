package com.generated.rescueStock.models;

/**
 * 交接事项：逐条登记的待办（待入库 / 临期批次 / 异常库存 / 其他），并指定接手人。
 * 新值班员领取交接单后逐项处理，全部 DONE 才允许关单。
 */
public class HandoverItem {
  public Long id;
  public Long handover_id;
  /** HandoverItemCategory */
  public String category;
  /** 事项内容描述，如批次号、物料、数量、异常说明 */
  public String content;
  /** 指定接手人 */
  public String assignee;
  /** 关联库存批次（可空），仅用于定位，交接处理不会回写批次库存 */
  public String batch_no;
  /** HandoverItemStatus: PENDING / DONE */
  public String status;
  public String processed_at;
  public String process_note;
  public Integer sort_no;
}
