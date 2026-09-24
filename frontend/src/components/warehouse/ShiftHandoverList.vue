<script setup lang="ts">
import ShiftHandoverDetail from "./ShiftHandoverDetail.vue";
import type { ShiftHandover } from "../../types/ShiftHandover";

defineProps<{
  active: ShiftHandover[];
  history: ShiftHandover[];
  saving: boolean;
  loading: boolean;
}>();

const emit = defineEmits<{
  (e: "claim", id: number, claimedBy: string): void;
  (e: "processItem", id: number, itemId: number, done: boolean, note: string): void;
  (e: "close", id: number, note: string): void;
  (e: "new"): void;
}>();
</script>

<template>
  <div class="panel wide handover-panel">
    <div class="handover-panel-head">
      <h2>值班交接</h2>
      <button class="btn btn-primary" @click="emit('new')">登记交接单</button>
    </div>

    <p v-if="loading" class="foot-hint">交接单加载中…</p>

    <template v-if="!loading">
      <section v-if="active.length" class="handover-section">
        <h3>当前有效交接单</h3>
        <p class="section-hint">新值班员领取后逐项处理，全部事项办结才能关闭；同一仓库重叠时段仅保留一张有效单。</p>
        <ShiftHandoverDetail
          v-for="handover in active"
          :key="handover.id"
          :handover="handover"
          :saving="saving"
          @claim="(id, claimedBy) => emit('claim', id, claimedBy)"
          @process-item="(id, itemId, done, note) => emit('processItem', id, itemId, done, note)"
          @close="(id, note) => emit('close', id, note)"
        />
      </section>

      <div v-else class="empty">当前仓库没有未关闭的交接单，可登记一张新的值班交接单。</div>

      <section v-if="history.length" class="handover-section history">
        <h3>历史交接（按仓库回看）</h3>
        <details v-for="handover in history" :key="handover.id" class="history-item">
          <summary>
            <span>#{{ handover.id }}</span>
            <span>{{ handover.shift_start_at }} ～ {{ handover.shift_end_at }}</span>
            <span>负责人 {{ handover.shift_owner }}</span>
            <span>领取 {{ handover.claimed_by || "—" }}</span>
            <span class="badge">已关闭</span>
          </summary>
          <ShiftHandoverDetail
            :handover="handover"
            :saving="true"
            @claim="emit('claim', $event, '')"
            @process-item="(id, itemId, done, note) => emit('processItem', id, itemId, done, note)"
            @close="(id, note) => emit('close', id, note)"
          />
        </details>
      </section>
    </template>
  </div>
</template>
