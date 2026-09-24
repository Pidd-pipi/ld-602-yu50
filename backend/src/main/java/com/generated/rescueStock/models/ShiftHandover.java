package com.generated.rescueStock.models;

import java.util.ArrayList;
import java.util.List;
import com.generated.rescueStock.constants.ShiftHandoverStatus;

/**
 * 值班交接单：针对单个仓库，绑定当班负责人与值守时段。
 * 同一仓库重叠时段只能存在一张非 CLOSED 的有效交接单。
 * 交接动作只记录事项处理状态，不修改任何库存批次数据。
 */
public class ShiftHandover {
  public Long id;
  public Long warehouse_id;
  /** 当班负责人（交班人） */
  public String shift_owner;
  /** 接班/领取人，领取前为空 */
  public String claimed_by;
  public String shift_start_at;
  public String shift_end_at;
  /** 值班备注 */
  public String remark;
  /** ShiftHandoverStatus: PENDING_CLAIM / CLAIMED / CLOSED */
  public String status;
  public String created_at;
  public String claimed_at;
  public String closed_at;
  public List<HandoverItem> items = new ArrayList<>();

  public boolean isActive() {
    return !ShiftHandoverStatus.CLOSED.equals(status);
  }
}
