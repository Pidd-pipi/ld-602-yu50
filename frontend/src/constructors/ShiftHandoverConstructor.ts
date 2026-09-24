import type { CreateShiftHandoverInput, HandoverItemInput, ShiftHandover } from "../types/ShiftHandover";

/** 交接单详情的默认结构（页面/store 不得散写默认对象）。 */
export const createDefaultShiftHandover = (overrides: Partial<ShiftHandover> = {}): ShiftHandover => ({
  id: 0,
  warehouse_id: 0,
  shift_owner: "",
  claimed_by: null,
  shift_start_at: "",
  shift_end_at: "",
  remark: "",
  status: "PENDING_CLAIM",
  created_at: null,
  claimed_at: null,
  closed_at: null,
  items: [],
  ...overrides
});

export const createShiftHandoverResponse = createDefaultShiftHandover;

/** 创建表单：逐条登记事项与接手人用的空白事项行。 */
export const createHandoverItemForm = (overrides: Partial<HandoverItemInput> = {}): HandoverItemInput => ({
  category: "INBOUND_PENDING",
  content: "",
  assignee: "",
  batch_no: "",
  ...overrides
});

export const createShiftHandoverForm = (overrides: Partial<CreateShiftHandoverInput> = {}): CreateShiftHandoverInput => ({
  warehouse_id: 0,
  shift_owner: "",
  shift_start_at: "",
  shift_end_at: "",
  remark: "",
  items: [createHandoverItemForm()],
  ...overrides
});
