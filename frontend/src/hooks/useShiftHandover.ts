import { computed, type Ref } from "vue";
import { useShiftHandoverStore } from "../stores/ShiftHandoverStore";
import type { ShiftHandover } from "../types/ShiftHandover";

/**
 * 值班交接流程：按仓库筛选交接单、统计待办与冲突提示。
 * 页面只负责交互，创建/领取/逐项处理/关闭的规则由 store+API 保证，失败时原单与库存不变。
 */
export function useShiftHandover(warehouseId: Ref<number>) {
  const store = useShiftHandoverStore();

  const handovers = computed<ShiftHandover[]>(() => store.byWarehouse(warehouseId.value));
  const activeHandovers = computed(() => handovers.value.filter((row) => row.status !== "CLOSED"));
  const closedHandovers = computed(() => handovers.value.filter((row) => row.status === "CLOSED"));

  const pendingClaim = computed(() => activeHandovers.value.find((row) => row.status === "PENDING_CLAIM") ?? null);
  const claimed = computed(() => activeHandovers.value.find((row) => row.status === "CLAIMED") ?? null);

  const pendingItemCount = computed(() =>
    activeHandovers.value.reduce(
      (sum, handover) => sum + handover.items.filter((item) => item.status !== "DONE").length,
      0
    )
  );

  const itemProgress = (handover: ShiftHandover) => {
    const total = handover.items.length;
    const done = handover.items.filter((item) => item.status === "DONE").length;
    return { total, done, percent: total === 0 ? 0 : Math.round((done / total) * 100) };
  };

  return {
    store,
    handovers,
    activeHandovers,
    closedHandovers,
    pendingClaim,
    claimed,
    pendingItemCount,
    itemProgress
  };
}
