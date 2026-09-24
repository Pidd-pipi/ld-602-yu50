<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import type { Warehouse } from "../../types/Warehouse";
import { useShiftHandoverStore } from "../../stores/ShiftHandoverStore";
import { useShiftHandover } from "../../hooks/useShiftHandover";
import { HandoverApiError } from "../../api/HandoverApiError";
import HandoverCard from "./HandoverCard.vue";
import HandoverFormDialog from "./HandoverFormDialog.vue";
import EmptyState from "../common/EmptyState.vue";
import type { HandoverSheet } from "../../types/HandoverSheet";
import type { HandoverItem } from "../../types/HandoverItem";

const props = defineProps<{ warehouses: Warehouse[] }>();

const store = useShiftHandoverStore();
const { codeOf, windowOf } = useShiftHandover();

const selectedId = ref<number>(props.warehouses[0]?.id ?? 0);
const formOpen = ref(false);
const toast = ref<{ kind: "ok" | "error"; text: string } | null>(null);
let toastTimer: ReturnType<typeof setTimeout> | undefined;

const showToast = (kind: "ok" | "error", text: string) => {
  toast.value = { kind, text };
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (toast.value = null), 4200);
};

const refresh = async () => {
  await store.load(selectedId.value || undefined);
};

onMounted(refresh);
watch(selectedId, refresh);

const activeSheets = computed(() => store.rows.filter((row) => row.status !== "CLOSED"));
const closedSheets = computed(() => store.rows.filter((row) => row.status === "CLOSED"));

const onSubmitted = (sheet: HandoverSheet) => showToast("ok", `${codeOf(sheet.id)} 已登记，等待新值班员领取`);

const onClaim = async ({ sheet, claimedBy }: { sheet: HandoverSheet; claimedBy: string }) => {
  try {
    await store.claim(sheet.id, claimedBy);
    showToast("ok", `${claimedBy} 已领取 ${codeOf(sheet.id)}，请逐项处理交接事项`);
  } catch (error) {
    showToast("error", error instanceof HandoverApiError ? error.message : "领取失败，请稍后重试");
  }
};

const onComplete = async ({ sheet, item, note }: { sheet: HandoverSheet; item: HandoverItem; note: string }) => {
  try {
    await store.completeItem(sheet.id, item.id, note);
    const title = item.content.length > 12 ? `${item.content.slice(0, 12)}…` : item.content;
    showToast("ok", `事项「${title}」已标记处理完成`);
  } catch (error) {
    showToast("error", error instanceof HandoverApiError ? error.message : "登记处理结果失败");
  }
};

const onClose = async (sheet: HandoverSheet) => {
  try {
    await store.close(sheet.id);
    showToast("ok", `${codeOf(sheet.id)} 全部事项处理完毕，已关闭归档`);
  } catch (error) {
    showToast("error", error instanceof HandoverApiError ? error.message : "关闭失败");
  }
};
</script>

<template>
  <section class="panel handover-panel">
    <header class="panel-head">
      <div>
        <h2>值班交接</h2>
        <p class="panel-sub">登记当班负责人、时段与逐条事项；同一仓库重叠时段仅允许一张有效交接单。</p>
      </div>
      <div class="panel-tools">
        <label class="warehouse-select">
          <span>仓库</span>
          <select v-model.number="selectedId">
            <option v-for="warehouse in warehouses" :key="warehouse.id" :value="warehouse.id">{{ warehouse.name }}</option>
          </select>
        </label>
        <button type="button" class="btn primary" @click="formOpen = true">+ 新建交接单</button>
      </div>
    </header>

    <div v-if="store.loading" class="panel-loading">加载交接记录中…</div>

    <template v-else>
      <h3 class="group-title">进行中（待领取 / 处理中）<em>{{ activeSheets.length }}</em></h3>
      <div v-if="activeSheets.length" class="handover-list">
        <HandoverCard v-for="sheet in activeSheets" :key="sheet.id" :sheet="sheet" @claim="onClaim" @complete="onComplete" @close="onClose" />
      </div>
      <EmptyState v-else text="当前仓库暂无进行中的交接单" />

      <h3 class="group-title">历史交接（重开页面仍可按仓库回看）<em>{{ closedSheets.length }}</em></h3>
      <table v-if="closedSheets.length" class="history-table">
        <thead>
          <tr><th>单号</th><th>交接时段</th><th>当班负责人</th><th>接手人</th><th>事项</th><th>关闭时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="sheet in closedSheets" :key="sheet.id">
            <td>{{ codeOf(sheet.id) }}</td>
            <td>{{ windowOf(sheet) }}</td>
            <td>{{ sheet.manager_name }}</td>
            <td>{{ sheet.claimed_by ?? "—" }}</td>
            <td>{{ sheet.items.filter((item) => item.status === "DONE").length }}/{{ sheet.items.length }}</td>
            <td>{{ sheet.closed_at ? new Date(sheet.closed_at).toLocaleString("zh-CN") : "—" }}</td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-else text="该仓库暂无历史交接记录" />
    </template>

    <transition name="toast">
      <div v-if="toast" class="toast" :data-kind="toast.kind" role="alert">
        <strong>{{ toast.kind === "ok" ? "操作成功" : "无法操作" }}</strong>
        <span>{{ toast.text }}</span>
      </div>
    </transition>

    <HandoverFormDialog
      v-model:open="formOpen"
      :warehouses="warehouses"
      :warehouse-id="selectedId"
      :existing="store.rows"
      @submitted="onSubmitted"
    />
  </section>
</template>
