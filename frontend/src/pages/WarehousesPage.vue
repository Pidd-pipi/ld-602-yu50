<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useWarehouseStore } from "../stores/WarehouseStore";
import { useInventoryBatchStore } from "../stores/InventoryBatchStore";
import HandoverPanel from "../components/warehouse/HandoverPanel.vue";
import StatCard from "../components/common/StatCard.vue";
import StatusBadge from "../components/common/StatusBadge.vue";
import type { Warehouse } from "../types/Warehouse";

const warehouseStore = useWarehouseStore();
const batchStore = useInventoryBatchStore();
const ready = ref(false);

onMounted(async () => {
  await Promise.all([warehouseStore.load(), batchStore.load()]);
  ready.value = true;
});

const warehouses = computed<Warehouse[]>(() => warehouseStore.rows as Warehouse[]);

const expireSoonIds = computed(() => {
  const horizon = Date.now() + 30 * 24 * 60 * 60 * 1000;
  return new Set(
    batchStore.rows
      .filter((batch) => {
        const expireAt = new Date(batch.expire_at).getTime();
        return Number.isFinite(expireAt) && expireAt <= horizon;
      })
      .map((batch) => batch.warehouse_id)
  );
});

const abnormalBatches = computed(() =>
  batchStore.rows.filter((batch) => !["NORMAL", "READY", "APPROVED", "SUBMITTED"].includes(batch.quality_status))
);

const pendingInboundCount = computed(() => batchStore.rows.filter((batch) => batch.quality_status === "DRAFT").length);
</script>

<template>
  <div v-if="!ready" class="page-loading">加载仓库数据中…</div>
  <div v-else class="warehouse-page">
    <section class="metrics">
      <StatCard label="在管仓库" :value="warehouses.length" />
      <StatCard label="待入库批次（示意）" :value="pendingInboundCount" />
      <StatCard label="异常库存批次（示意）" :value="abnormalBatches.length" />
    </section>

    <section class="panel warehouse-overview">
      <header class="panel-head">
        <div>
          <h2>仓库列表</h2>
          <p class="panel-sub">待处理入库、临期批次与异常库存请在下方值班交接中逐条登记，避免群消息遗漏。</p>
        </div>
      </header>
      <div class="warehouse-grid">
        <article v-for="warehouse in warehouses" :key="warehouse.id" class="warehouse-tile">
          <div class="tile-head">
            <strong>{{ warehouse.name }}</strong>
            <StatusBadge :value="warehouse.status" tone="info" />
          </div>
          <p class="tile-line">{{ warehouse.district }} · {{ warehouse.address }}</p>
          <p class="tile-line">容量等级：{{ warehouse.capacity_level }} ｜ 联系电话：{{ warehouse.contact_phone }}</p>
          <p v-if="expireSoonIds.has(warehouse.id)" class="tile-warn">⚠ 存在 30 天内临期批次，交接时重点说明</p>
        </article>
      </div>
    </section>

    <HandoverPanel :warehouses="warehouses" />
  </div>
</template>
