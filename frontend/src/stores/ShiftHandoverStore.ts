import { defineStore } from "pinia";
import type { HandoverSheet, HandoverCreatePayload } from "../types/HandoverSheet";
import {
  listHandovers,
  createHandover,
  claimHandover,
  completeHandoverItem,
  closeHandover
} from "../api/ShiftHandover";

interface State {
  rows: HandoverSheet[];
  warehouseId: number | null;
  loading: boolean;
  lastActionAt: string | null;
}

export const useShiftHandoverStore = defineStore("shiftHandover", {
  state: (): State => ({ rows: [], warehouseId: null, loading: false, lastActionAt: null }),
  getters: {
    pending: (state) => state.rows.filter((row) => row.status === "PENDING"),
    active: (state) => state.rows.filter((row) => row.status !== "CLOSED"),
    closed: (state) => state.rows.filter((row) => row.status === "CLOSED"),
    byId: (state) => (id: number) => state.rows.find((row) => row.id === id)
  },
  actions: {
    async load(warehouseId?: number) {
      this.loading = true;
      this.warehouseId = warehouseId ?? null;
      try {
        this.rows = await listHandovers(warehouseId);
      } finally {
        this.loading = false;
      }
    },
    async create(payload: HandoverCreatePayload): Promise<HandoverSheet> {
      const sheet = await createHandover(payload);
      await this.load(payload.warehouse_id);
      this.lastActionAt = new Date().toISOString();
      return sheet;
    },
    async claim(id: number, claimedBy: string) {
      await claimHandover(id, claimedBy);
      await this.load(this.warehouseId ?? undefined);
      this.lastActionAt = new Date().toISOString();
    },
    async completeItem(id: number, itemId: number, note: string) {
      await completeHandoverItem(id, itemId, note);
      await this.load(this.warehouseId ?? undefined);
      this.lastActionAt = new Date().toISOString();
    },
    async close(id: number) {
      await closeHandover(id);
      await this.load(this.warehouseId ?? undefined);
      this.lastActionAt = new Date().toISOString();
    }
  }
});
