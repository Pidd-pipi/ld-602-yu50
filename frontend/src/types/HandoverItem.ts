import type { HandoverItemType } from "../constants/HandoverItemType";
import type { HandoverItemStatus } from "../constants/HandoverItemStatus";

export interface HandoverItem {
  id: number;
  type: HandoverItemType;
  content: string;
  assignee: string;
  status: HandoverItemStatus;
  result_note: string;
  completed_at: string | null;
}

export interface HandoverItemInput {
  type: HandoverItemType;
  content: string;
  assignee: string;
}
