import { defineStore } from "pinia";
import {
  claimShiftHandover,
  closeShiftHandover,
  createShiftHandover,
  HandoverApiError,
  listShiftHandover,
  processHandoverItem
} from "../api/ShiftHandover";
import type { CreateShiftHandoverInput, ShiftHandover } from "../types/ShiftHandover";

/** 值班交接 store：所有写操作失败（冲突/已领取/未办结）时不改本地状态，错误原因透传给页面。 */
export const useShiftHandoverStore = defineStore("shiftHandover", {
  state: () => ({
    rows: [] as ShiftHandover[],
    loading: false,
    saving: false,
    error: "" as string
  }),
  getters: {
    byWarehouse: (state) => (warehouseId: number) =>
      state.rows
        .filter((row) => row.warehouse_id === warehouseId)
        .sort((a, b) => b.shift_start_at.localeCompare(a.shift_start_at)),
    byId: (state) => (id: number) => state.rows.find((row) => row.id === id)
  },
  actions: {
    clearError() {
      this.error = "";
    },
    async load(warehouseId?: number) {
      this.loading = true;
      this.error = "";
      try {
        this.rows = await listShiftHandover(warehouseId);
      } catch (error) {
        this.error = error instanceof HandoverApiError ? error.message : "交接单加载失败。";
      } finally {
        this.loading = false;
      }
    },
    async create(input: CreateShiftHandoverInput): Promise<ShiftHandover | null> {
      this.saving = true;
      this.error = "";
      try {
        const created = await createShiftHandover(input);
        this.upsert(created);
        return created;
      } catch (error) {
        this.error = error instanceof HandoverApiError ? error.message : "交接单创建失败。";
        return null;
      } finally {
        this.saving = false;
      }
    },
    async claim(id: number, claimedBy: string): Promise<boolean> {
      this.saving = true;
      this.error = "";
      try {
        this.upsert(await claimShiftHandover(id, claimedBy));
        return true;
      } catch (error) {
        this.error = error instanceof HandoverApiError ? error.message : "领取交接单失败。";
        return false;
      } finally {
        this.saving = false;
      }
    },
    async processItem(id: number, itemId: number, status: string, note: string): Promise<boolean> {
      this.saving = true;
      this.error = "";
      try {
        this.upsert(await processHandoverItem(id, itemId, status, note));
        return true;
      } catch (error) {
        this.error = error instanceof HandoverApiError ? error.message : "事项处理失败。";
        return false;
      } finally {
        this.saving = false;
      }
    },
    async close(id: number, note: string): Promise<boolean> {
      this.saving = true;
      this.error = "";
      try {
        this.upsert(await closeShiftHandover(id, note));
        return true;
      } catch (error) {
        this.error = error instanceof HandoverApiError ? error.message : "关闭交接单失败。";
        return false;
      } finally {
        this.saving = false;
      }
    },
    upsert(handover: ShiftHandover) {
      const index = this.rows.findIndex((row) => row.id === handover.id);
      if (index >= 0) this.rows.splice(index, 1, handover);
      else this.rows.unshift(handover);
    }
  }
});
