<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import StatCard from "../components/common/StatCard.vue";
import StatusBadge from "../components/common/StatusBadge.vue";
import BatchTable from "../components/common/BatchTable.vue";
import ExpireWarningList from "../components/common/ExpireWarningList.vue";
import ShiftHandoverForm from "../components/warehouse/ShiftHandoverForm.vue";
import ShiftHandoverList from "../components/warehouse/ShiftHandoverList.vue";
import { useShiftHandover } from "../hooks/useShiftHandover";
import { useInventoryBatchStore } from "../stores/InventoryBatchStore";
import { useSupplyItemStore } from "../stores/SupplyItemStore";
import { useWarehouseStore } from "../stores/WarehouseStore";
import type { CreateShiftHandoverInput } from "../types/ShiftHandover";

const warehouseStore = useWarehouseStore();
const batchStore = useInventoryBatchStore();
const supplyItemStore = useSupplyItemStore();

const selectedWarehouseId = ref<number>(1);
const showCreate = ref(false);
const actionMessage = ref("");
let messageTimer: ReturnType<typeof setTimeout> | undefined;

const {
  store: handoverStore,
  activeHandovers,
  closedHandovers,
  pendingItemCount
} = useShiftHandover(selectedWarehouseId);

const warehouses = computed(() => warehouseStore.rows);
const selectedWarehouse = computed(() => warehouses.value.find((w) => w.id === selectedWarehouseId.value));

const batches = computed(() =>
  batchStore.rows.filter((batch) => batch.warehouse_id === selectedWarehouseId.value)
);

const warehouseStatusText: Record<string, string> = {
  ACTIVE: "启用",
  DISABLED: "停用",
  SUBMITTED: "启用",
  APPROVED: "启用",
  DRAFT: "待核验"
};

function flashMessage(text: string) {
  actionMessage.value = text;
  clearTimeout(messageTimer);
  messageTimer = setTimeout(() => (actionMessage.value = ""), 5000);
}

async function selectWarehouse(id: number) {
  selectedWarehouseId.value = id;
  await handoverStore.load(id);
}

watch(
  () => handoverStore.error,
  (error) => {
    if (error) flashMessage(error);
  },
  { immediate: true }
);

async function handleCreate(payload: CreateShiftHandoverInput) {
  const created = await handoverStore.create(payload);
  if (created) {
    showCreate.value = false;
    flashMessage(`交接单 #${created.id} 已登记，等待新值班员领取。`);
  }
}

async function handleClaim(id: number, claimedBy: string) {
  if (await handoverStore.claim(id, claimedBy)) {
    flashMessage("已领取交接单，请逐项处理交接事项。");
  }
}

async function handleProcessItem(id: number, itemId: number, done: boolean, note: string) {
  if (await handoverStore.processItem(id, itemId, done ? "DONE" : "PENDING", note)) {
    flashMessage(done ? "事项已标记为已处理。" : "事项已撤回为待处理。");
  }
}

async function handleClose(id: number, note: string) {
  if (await handoverStore.close(id, note)) {
    showCreate.value = false;
    flashMessage(`交接单 #${id} 已关闭。`);
  }
}

onMounted(async () => {
  await Promise.all([warehouseStore.load(), batchStore.load(), supplyItemStore.load()]);
  if (warehouseStore.rows.length && !warehouseStore.rows.some((w) => w.id === selectedWarehouseId.value)) {
    selectedWarehouseId.value = warehouseStore.rows[0].id;
  }
  await handoverStore.load(selectedWarehouseId.value);
});
</script>

<template>
  <section class="warehouse-page">
    <div class="warehouse-tabs">
      <button
        v-for="warehouse in warehouses"
        :key="warehouse.id"
        type="button"
        :class="{ active: warehouse.id === selectedWarehouseId }"
        @click="selectWarehouse(warehouse.id)"
      >
        {{ warehouse.name }}
      </button>
    </div>

    <transition name="banner">
      <div v-if="actionMessage" class="action-banner" role="alert">
        <span>{{ actionMessage }}</span>
        <button type="button" class="banner-close" @click="actionMessage = ''">×</button>
      </div>
    </transition>

    <header class="warehouse-head panel">
      <div>
        <h2>{{ selectedWarehouse?.name ?? "仓库库存" }}</h2>
        <p class="warehouse-address">
          {{ selectedWarehouse?.district }} · {{ selectedWarehouse?.address }}
          ｜仓管员 #{{ selectedWarehouse?.manager_id }} ｜{{ selectedWarehouse?.contact_phone }}
        </p>
      </div>
      <StatusBadge :value="warehouseStatusText[selectedWarehouse?.status ?? ''] ?? '启用'" />
    </header>

    <section class="metrics">
      <StatCard label="在管批次" :value="batches.length" />
      <StatCard label="30 天内临期" :value="batches.filter((b) => new Date(b.expire_at).getTime() <= Date.now() + 30 * 864e5).length" />
      <StatCard label="待处理交接事项" :value="pendingItemCount" />
    </section>

    <section class="workbench warehouse-grid">
      <BatchTable
        title="本仓库存批次"
        :batches="batches"
        :supply-items="supplyItemStore.rows"
      />
      <ExpireWarningList
        title="临期批次提醒"
        :batches="batches"
        :supply-items="supplyItemStore.rows"
        :within-days="30"
      />
    </section>

    <section v-if="showCreate" class="workbench">
      <ShiftHandoverForm
        :warehouses="warehouses"
        :warehouse-id="selectedWarehouseId"
        :saving="handoverStore.saving"
        :active-handovers="activeHandovers"
        @submit="handleCreate"
        @cancel="showCreate = false"
      />
    </section>

    <ShiftHandoverList
      :active="activeHandovers"
      :history="closedHandovers"
      :saving="handoverStore.saving"
      :loading="handoverStore.loading"
      @new="showCreate = true"
      @claim="handleClaim"
      @process-item="handleProcessItem"
      @close="handleClose"
    />
  </section>
</template>
