export interface HandoverItem {
  id: number;
  handover_id: number;
  /** HandoverItemCategory: INBOUND_PENDING / EXPIRING_BATCH / ABNORMAL_INVENTORY / OTHER */
  category: string;
  content: string;
  /** 指定接手人 */
  assignee: string;
  batch_no?: string | null;
  /** HandoverItemStatus: PENDING / DONE */
  status: string;
  processed_at?: string | null;
  process_note?: string | null;
  sort_no?: number;
}

export interface ShiftHandover {
  id: number;
  warehouse_id: number;
  /** 当班负责人（交班人） */
  shift_owner: string;
  /** 领取交接单的新值班员，未领取时为空 */
  claimed_by?: string | null;
  shift_start_at: string;
  shift_end_at: string;
  remark?: string | null;
  /** ShiftHandoverStatus: PENDING_CLAIM / CLAIMED / CLOSED */
  status: string;
  created_at?: string | null;
  claimed_at?: string | null;
  closed_at?: string | null;
  items: HandoverItem[];
}

/** 创建交接单时提交的事项条目。 */
export interface HandoverItemInput {
  category: string;
  content: string;
  assignee: string;
  batch_no?: string | null;
}

export interface CreateShiftHandoverInput {
  warehouse_id: number;
  shift_owner: string;
  shift_start_at: string;
  shift_end_at: string;
  remark?: string | null;
  items: HandoverItemInput[];
}
