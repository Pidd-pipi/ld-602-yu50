import type { HandoverStatus } from "../constants/HandoverStatus";
import type { HandoverItem, HandoverItemInput } from "./HandoverItem";

export interface HandoverSheet {
  id: number;
  warehouse_id: number;
  manager_name: string;
  shift_start: string;
  shift_end: string;
  status: HandoverStatus;
  claimed_by: string | null;
  claimed_at: string | null;
  created_at: string;
  closed_at: string | null;
  items: HandoverItem[];
}

export interface HandoverCreatePayload {
  warehouse_id: number;
  manager_name: string;
  shift_start: string;
  shift_end: string;
  items: HandoverItemInput[];
}
