<script setup lang="ts">
import { computed } from "vue";
import StatusBadge from "./StatusBadge.vue";
import type { InventoryBatch } from "../../types/InventoryBatch";
import type { SupplyItem } from "../../types/SupplyItem";
import { formatDate } from "../../utils/formatters";

const props = defineProps<{
  title?: string;
  batches: InventoryBatch[];
  supplyItems?: SupplyItem[];
  onlyExpiring?: boolean;
  expireWithinDays?: number;
}>();

const qualityText: Record<string, string> = {
  READY: "正常",
  PENDING: "待检",
  DAMAGED: "破损",
  SUBMITTED: "待检",
  APPROVED: "正常",
  DRAFT: "待检"
};

const supplyName = (id: number) => props.supplyItems?.find((item) => item.id === id)?.name ?? `物资 #${id}`;

const rows = computed(() => {
  if (!props.onlyExpiring) return props.batches;
  const days = props.expireWithinDays ?? 30;
  const limit = Date.now() + days * 24 * 60 * 60 * 1000;
  return props.batches
    .filter((batch) => {
      const expireAt = new Date(batch.expire_at).getTime();
      return !Number.isNaN(expireAt) && expireAt <= limit;
    })
    .sort((a, b) => a.expire_at.localeCompare(b.expire_at));
});
</script>

<template>
  <div class="shared-widget batch-table-widget">
    <strong>{{ title ?? "批次明细" }}</strong>
    <span v-if="onlyExpiring" class="badge warn">{{ rows.length }} 条临期</span>
    <table v-if="rows.length" class="data-table">
      <thead>
        <tr>
          <th>批次号</th><th>物资</th><th>数量</th><th>到期时间</th><th>来源</th><th>质量</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="batch in rows" :key="batch.id">
          <td>{{ batch.batch_no }}</td>
          <td>{{ supplyName(batch.supply_item_id) }}</td>
          <td>{{ batch.quantity }}</td>
          <td :class="{ 'expire-soon': onlyExpiring }">{{ formatDate(batch.expire_at) }}</td>
          <td>{{ batch.inbound_source }}</td>
          <td><StatusBadge :value="qualityText[batch.quality_status] ?? batch.quality_status" /></td>
        </tr>
      </tbody>
    </table>
    <div v-else class="empty">暂无批次</div>
  </div>
</template>
