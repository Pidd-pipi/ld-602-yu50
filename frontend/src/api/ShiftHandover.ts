import type { HandoverSheet, HandoverCreatePayload } from "../types/HandoverSheet";
import { HandoverApiError } from "./HandoverApiError";
import { localHandoverEngine } from "../mocks/handoverLocalEngine";
import { ERROR_MESSAGES } from "../constants/errorMessages";
import { HandoverStatusText } from "../constants/HandoverStatus";
import { formatHandoverCode, formatShiftWindow } from "../utils/formatters";

const endpoint = "/api/shift-handover";

interface ErrorBody {
  code: string;
  message: string;
  meta?: Record<string, string | number>;
}

/** 后端仅返回错误码与参数，用户可读文案由前端错误消息模板统一渲染 */
const resolveMessage = (body: ErrorBody): string => {
  const template = ERROR_MESSAGES[body.code as keyof typeof ERROR_MESSAGES];
  if (!template) return body.message || "操作失败，请稍后重试";
  const meta = body.meta ?? {};
  return template.replace(/\{(\w+)\}/g, (_, key: string) => {
    if (key === "code") {
      const id = meta.conflict_id ?? meta.id;
      return id === undefined ? "{code}" : formatHandoverCode(Number(id));
    }
    if (key === "window") {
      return meta.conflict_start && meta.conflict_end
        ? formatShiftWindow(String(meta.conflict_start), String(meta.conflict_end))
        : "{window}";
    }
    if (key === "status") {
      return HandoverStatusText[String(meta.status) as keyof typeof HandoverStatusText] ?? String(meta.status ?? "");
    }
    const value = meta[key];
    return value === undefined ? `{${key}}` : String(value);
  });
};

const request = async <T>(path: string, init?: RequestInit): Promise<T> => {
  const res = await fetch(`${endpoint}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...init
  });
  if (!res.ok) {
    let body: ErrorBody | null = null;
    try {
      body = (await res.json()) as ErrorBody;
    } catch {
      // 非 JSON 错误响应
    }
    if (body?.code) throw new HandoverApiError(body.code, resolveMessage(body), body.meta);
    throw new HandoverApiError("VALIDATION_FAILED", `服务暂不可用（HTTP ${res.status}）`);
  }
  return (await res.json()) as T;
};

const toError = (cause: unknown, fallbackMessage: string): HandoverApiError => {
  if (cause instanceof HandoverApiError) {
    return new HandoverApiError(cause.code, resolveMessage({ code: cause.code, message: cause.message, meta: cause.meta }), cause.meta);
  }
  return new HandoverApiError("LOCAL_FALLBACK", fallbackMessage);
};

export async function listHandovers(warehouseId?: number): Promise<HandoverSheet[]> {
  try {
    const query = warehouseId === undefined ? "" : `?warehouseId=${warehouseId}`;
    return await request<HandoverSheet[]>(query);
  } catch (error) {
    if (error instanceof HandoverApiError) throw error;
    // 后端离线时使用本地持久化数据，交接内容重开页面仍可按仓库回看
    return localHandoverEngine.list(warehouseId);
  }
}

export async function createHandover(payload: HandoverCreatePayload): Promise<HandoverSheet> {
  try {
    return await request<HandoverSheet>("", { method: "POST", body: JSON.stringify(payload) });
  } catch (error) {
    if (error instanceof HandoverApiError) throw error;
    try {
      return localHandoverEngine.create(payload);
    } catch (cause) {
      throw toError(cause, "本地保存交接单失败");
    }
  }
}

export async function claimHandover(id: number, claimedBy: string): Promise<HandoverSheet> {
  try {
    return await request<HandoverSheet>(`/${id}/claim`, { method: "POST", body: JSON.stringify({ claimed_by: claimedBy }) });
  } catch (error) {
    if (error instanceof HandoverApiError) throw error;
    try {
      return localHandoverEngine.claim(id, claimedBy);
    } catch (cause) {
      throw toError(cause, "本地领取交接单失败");
    }
  }
}

export async function completeHandoverItem(id: number, itemId: number, note: string): Promise<HandoverSheet> {
  try {
    return await request<HandoverSheet>(`/${id}/items/${itemId}/complete`, {
      method: "POST",
      body: JSON.stringify({ result_note: note })
    });
  } catch (error) {
    if (error instanceof HandoverApiError) throw error;
    try {
      return localHandoverEngine.completeItem(id, itemId, note);
    } catch (cause) {
      throw toError(cause, "本地登记事项结果失败");
    }
  }
}

export async function closeHandover(id: number): Promise<HandoverSheet> {
  try {
    return await request<HandoverSheet>(`/${id}/close`, { method: "POST" });
  } catch (error) {
    if (error instanceof HandoverApiError) throw error;
    try {
      return localHandoverEngine.close(id);
    } catch (cause) {
      throw toError(cause, "本地关闭交接单失败");
    }
  }
}

export { formatShiftWindow };
