<script setup lang="ts">
import { computed } from "vue";
import type { InventoryBatch } from "../../types/InventoryBatch";
import type { SupplyItem } from "../../types/SupplyItem";
import { formatDate } from "../../utils/formatters";

const props = defineProps<{
  title?: string;
  batches: InventoryBatch[];
  supplyItems?: SupplyItem[];
  withinDays?: number;
}>();

const days = computed(() => props.withinDays ?? 30);

const warnings = computed(() => {
  const now = Date.now();
  const limit = now + days.value * 24 * 60 * 60 * 1000;
  return props.batches
    .map((batch) => {
      const expireAt = new Date(batch.expire_at).getTime();
      const leftDays = Number.isNaN(expireAt) ? Infinity : Math.ceil((expireAt - now) / (24 * 60 * 60 * 1000));
      return { batch, leftDays };
    })
    .filter(({ leftDays }) => leftDays <= days.value)
    .sort((a, b) => a.leftDays - b.leftDays);
});

const supplyName = (id: number) => props.supplyItems?.find((item) => item.id === id)?.name ?? `物资 #${id}`;
const urgencyClass = (left: number) => (left <= 7 ? "critical" : left <= 15 ? "warning" : "notice");
</script>

<template>
  <div class="shared-widget expire-widget">
    <strong>{{ title ?? "临期预警" }}</strong>
    <ul v-if="warnings.length" class="expire-list">
      <li v-for="{ batch, leftDays } in warnings" :key="batch.id" :class="urgencyClass(leftDays)">
        <span class="expire-batch">{{ batch.batch_no }} · {{ supplyName(batch.supply_item_id) }}</span>
        <span class="expire-date">{{ formatDate(batch.expire_at) }}</span>
        <span class="expire-left">剩 {{ leftDays }} 天</span>
      </li>
    </ul>
    <div v-else class="empty">{{ days }} 天内没有临期批次</div>
  </div>
</template>
