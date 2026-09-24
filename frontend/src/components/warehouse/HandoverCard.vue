<script setup lang="ts">
import { ref } from "vue";
import type { HandoverSheet } from "../../types/HandoverSheet";
import StatusBadge from "../common/StatusBadge.vue";
import { useShiftHandover } from "../../hooks/useShiftHandover";
import { HandoverStatusText, HandoverStatusTone } from "../../constants/HandoverStatus";
import { HandoverItemStatusText } from "../../constants/HandoverItemStatus";
import type { HandoverItem } from "../../types/HandoverItem";

const props = defineProps<{ sheet: HandoverSheet }>();
const emit = defineEmits<{
  (e: "claim", payload: { sheet: HandoverSheet; claimedBy: string }): void;
  (e: "complete", payload: { sheet: HandoverSheet; item: HandoverItem; note: string }): void;
  (e: "close", sheet: HandoverSheet): void;
}>();

const { progressOf, codeOf, windowOf, timeOf, typeLabel, canClose } = useShiftHandover();
const progress = progressOf(props.sheet);

const claimName = ref("");
const completingId = ref<number | null>(null);
const completeNote = ref("");

const startComplete = (item: HandoverItem) => {
  completingId.value = item.id;
  completeNote.value = "";
};
const cancelComplete = () => {
  completingId.value = null;
  completeNote.value = "";
};
const confirmComplete = (item: HandoverItem) => {
  if (!completeNote.value.trim()) return;
  emit("complete", { sheet: props.sheet, item, note: completeNote.value });
  cancelComplete();
};
</script>

<template>
  <article class="handover-card" :data-status="sheet.status">
    <header class="handover-head">
      <div>
        <strong class="handover-code">{{ codeOf(sheet.id) }}</strong>
        <StatusBadge :value="sheet.status" :label="HandoverStatusText[sheet.status]" :tone="HandoverStatusTone[sheet.status]" />
      </div>
      <span class="handover-window">{{ windowOf(sheet) }}</span>
    </header>

    <dl class="handover-meta">
      <div><dt>当班负责人</dt><dd>{{ sheet.manager_name }}</dd></div>
      <div><dt>接手值班员</dt><dd>{{ sheet.claimed_by ?? "待领取" }}</dd></div>
      <div><dt>创建时间</dt><dd>{{ timeOf(sheet.created_at) }}</dd></div>
      <div v-if="sheet.closed_at"><dt>关闭时间</dt><dd>{{ timeOf(sheet.closed_at) }}</dd></div>
    </dl>

    <div class="handover-progress">
      <div class="progress-bar"><span :style="{ width: `${progress.percent}%` }" /></div>
      <span class="progress-text">事项 {{ progress.done }}/{{ progress.total }} 已处理<span v-if="progress.pending">，剩余 {{ progress.pending }} 项</span></span>
    </div>

    <ul class="handover-items">
      <li v-for="item in sheet.items" :key="item.id" class="handover-item" :data-done="item.status === 'DONE'">
        <div class="item-main">
          <span class="item-type">{{ typeLabel(item.type) }}</span>
          <p class="item-content">{{ item.content }}</p>
          <p class="item-assignee">接手人：{{ item.assignee }}</p>
          <p v-if="item.status === 'DONE'" class="item-result">处理结果：{{ item.result_note }}（{{ timeOf(item.completed_at) }}）</p>
        </div>
        <div class="item-side">
          <StatusBadge :value="item.status" :label="HandoverItemStatusText[item.status]" :tone="item.status === 'DONE' ? 'ok' : 'warn'" />
          <template v-if="sheet.status === 'CLAIMED' && item.status === 'PENDING'">
            <button v-if="completingId !== item.id" type="button" class="text-btn" @click="startComplete(item)">登记处理结果</button>
            <div v-else class="complete-inline">
              <textarea v-model="completeNote" rows="2" placeholder="填写处理结果后标记完成" />
              <div class="inline-actions">
                <button type="button" class="btn tiny" :disabled="!completeNote.trim()" @click="confirmComplete(item)">完成</button>
                <button type="button" class="btn tiny ghost" @click="cancelComplete">取消</button>
              </div>
            </div>
          </template>
        </div>
      </li>
    </ul>

    <footer class="handover-foot">
      <span v-if="sheet.status === 'PENDING'" class="foot-hint">新值班员确认信息无误后领取交接单</span>
      <span v-else-if="sheet.status === 'CLAIMED'" class="foot-hint" :class="{ blocked: !canClose(sheet) }">
        {{ canClose(sheet) ? "全部事项已处理，可以关闭交接单" : `还有 ${progress.pending} 项未处理，暂不能关闭` }}
      </span>
      <span v-else class="foot-hint">交接单已关闭归档，仅支持回看</span>

      <div v-if="sheet.status === 'PENDING'" class="claim-inline">
        <input v-model="claimName" maxlength="20" placeholder="领取人姓名" @keyup.enter="claimName.trim() && emit('claim', { sheet, claimedBy: claimName.trim() })" />
        <button type="button" class="btn primary tiny" :disabled="!claimName.trim()" @click="emit('claim', { sheet, claimedBy: claimName.trim() })">领取</button>
      </div>
      <button
        v-else-if="sheet.status === 'CLAIMED'"
        type="button"
        class="btn primary"
        :disabled="!canClose(sheet)"
        @click="emit('close', sheet)"
      >
        全部处理完，关闭交接单
      </button>
    </footer>
  </article>
</template>
