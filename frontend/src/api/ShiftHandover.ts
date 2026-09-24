import type { CreateShiftHandoverInput, ShiftHandover } from "../types/ShiftHandover";
import {
  isHandoverRuleError,
  localClaimShiftHandover,
  localCloseShiftHandover,
  localCreateShiftHandover,
  localListShiftHandover,
  localProcessHandoverItem,
  type HandoverRuleError
} from "../mocks/shiftHandoverStore";

const endpoint = "/api/shift-handover";

/** 后端返回的业务错误体，与 ShiftHandoverDtoFactory.errorDto 对齐。 */
export class HandoverApiError extends Error {
  code: string;
  status: number;
  constructor(code: string, message: string, status: number) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

type HandoverResult = ShiftHandover | HandoverRuleError;

async function send<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json" },
    ...init
  });
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    const code = body?.code ?? "UNKNOWN";
    const message = body?.message ?? "请求失败，请稍后重试。";
    throw new HandoverApiError(code, message, res.status);
  }
  return body as T;
}

function unwrap(result: HandoverResult): ShiftHandover {
  if (isHandoverRuleError(result)) throw new HandoverApiError(result.code, result.message, 409);
  return result;
}

/** 交接内容重开页面后按仓库回看：GET /api/shift-handover?warehouse_id= */
export async function listShiftHandover(warehouseId?: number): Promise<ShiftHandover[]> {
  try {
    const query = warehouseId ? `?warehouse_id=${warehouseId}` : "";
    return await send<ShiftHandover[]>(`${endpoint}${query}`);
  } catch (error) {
    if (error instanceof HandoverApiError) throw error;
    // 后端不可达时使用本地持久化数据，保证评审/离线场景可用
    return localListShiftHandover(warehouseId ?? null);
  }
}

export async function createShiftHandover(input: CreateShiftHandoverInput): Promise<ShiftHandover> {
  try {
    return await send<ShiftHandover>(endpoint, { method: "POST", body: JSON.stringify(input) });
  } catch (error) {
    if (error instanceof HandoverApiError) throw error;
    return unwrap(localCreateShiftHandover(input));
  }
}

export async function claimShiftHandover(id: number, claimedBy: string): Promise<ShiftHandover> {
  try {
    return await send<ShiftHandover>(`${endpoint}/${id}/claim`, {
      method: "POST",
      body: JSON.stringify({ claimed_by: claimedBy })
    });
  } catch (error) {
    if (error instanceof HandoverApiError) throw error;
    return unwrap(localClaimShiftHandover(id, claimedBy));
  }
}

export async function processHandoverItem(
  id: number,
  itemId: number,
  status: string,
  processNote = ""
): Promise<ShiftHandover> {
  try {
    return await send<ShiftHandover>(`${endpoint}/${id}/items/${itemId}`, {
      method: "PUT",
      body: JSON.stringify({ status, process_note: processNote })
    });
  } catch (error) {
    if (error instanceof HandoverApiError) throw error;
    return unwrap(localProcessHandoverItem(id, itemId, status, processNote));
  }
}

export async function closeShiftHandover(id: number, closeNote = ""): Promise<ShiftHandover> {
  try {
    return await send<ShiftHandover>(`${endpoint}/${id}/close`, {
      method: "POST",
      body: JSON.stringify({ close_note: closeNote })
    });
  } catch (error) {
    if (error instanceof HandoverApiError) throw error;
    return unwrap(localCloseShiftHandover(id, closeNote));
  }
}
