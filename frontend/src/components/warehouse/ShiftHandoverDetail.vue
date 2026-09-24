<script setup lang="ts">
import { computed, ref } from "vue";
import StatusBadge from "../common/StatusBadge.vue";
import { HandoverItemCategoryText } from "../../constants/HandoverItemCategory";
import { ShiftHandoverStatusText } from "../../constants/ShiftHandoverStatus";
import { getLastOperator, saveLastOperator } from "../../mocks/shiftHandoverStore";
import type { ShiftHandover } from "../../types/ShiftHandover";
import { formatDate } from "../../utils/formatters";

const props = defineProps<{
  handover: ShiftHandover;
  saving: boolean;
}>();

const emit = defineEmits<{
  (e: "claim", id: number, claimedBy: string): void;
  (e: "processItem", id: number, itemId: number, done: boolean, note: string): void;
  (e: "close", id: number, note: string): void;
}>();

const claimName = ref(getLastOperator());
const closeNote = ref("");
const processingNote = ref<Record<number, string>>({});
const showCloseBox = ref(false);

const doneCount = computed(() => props.handover.items.filter((item) => item.status === "DONE").length);
const totalCount = computed(() => props.handover.items.length);
const allDone = computed(() => totalCount.value > 0 && doneCount.value === totalCount.value);
const percent = computed(() => (totalCount.value === 0 ? 0 : Math.round((doneCount.value / totalCount.value) * 100)));

const isPending = computed(() => props.handover.status === "PENDING_CLAIM");
const isClaimed = computed(() => props.handover.status === "CLAIMED");
const isClosed = computed(() => props.handover.status === "CLOSED");
const statusClass = computed(() => ({
  PENDING_CLAIM: "is-pending",
  CLAIMED: "is-claimed",
  CLOSED: "is-closed"
}[props.handover.status] ?? ""));

function doClaim() {
  const name = claimName.value.trim();
  if (!name) return;
  saveLastOperator(name);
  emit("claim", props.handover.id, name);
}

function toggleItem(itemId: number, done: boolean) {
  emit("processItem", props.handover.id, itemId, done, processingNote.value[itemId] ?? "");
}

function doClose() {
  emit("close", props.handover.id, closeNote.value);
}
</script>

<template>
  <article class="handover-card" :class="statusClass">
    <header class="handover-card-head">
      <div>
        <span class="handover-no">交接单 #{{ handover.id }}</span>
        <StatusBadge :value="ShiftHandoverStatusText[handover.status as keyof typeof ShiftHandoverStatusText] ?? handover.status" />
      </div>
      <div class="handover-time">{{ formatDate(handover.shift_start_at) }} ～ {{ formatDate(handover.shift_end_at) }}</div>
    </header>

    <dl class="handover-meta">
      <div><dt>当班负责人</dt><dd>{{ handover.shift_owner }}</dd></div>
      <div><dt>领取值班员</dt><dd>{{ handover.claimed_by || "—" }}</dd></div>
      <div v-if="handover.claimed_at"><dt>领取时间</dt><dd>{{ formatDate(handover.claimed_at) }}</dd></div>
      <div v-if="handover.closed_at"><dt>关闭时间</dt><dd>{{ formatDate(handover.closed_at) }}</dd></div>
    </dl>
    <p v-if="handover.remark" class="handover-remark">{{ handover.remark }}</p>

    <!-- 待领取：新值班员登记姓名后领取，领取前不能处理事项 -->
    <div v-if="isPending" class="claim-row">
      <input v-model="claimName" type="text" placeholder="输入领取值班员姓名" :disabled="saving" />
      <button class="btn btn-primary" :disabled="saving || !claimName.trim()" @click="doClaim">
        {{ saving ? "处理中…" : "我是接班值班员，领取交接单" }}
      </button>
    </div>

    <div class="progress-row">
      <div class="progress-track"><div class="progress-fill" :style="{ width: `${percent}%` }"></div></div>
      <span>事项进度 {{ doneCount }}/{{ totalCount }}</span>
    </div>

    <ul class="item-list">
      <li v-for="item in handover.items" :key="item.id" class="item-line" :class="{ done: item.status === 'DONE' }">
        <div class="item-line-head">
          <span class="item-category-tag">{{ HandoverItemCategoryText[item.category as keyof typeof HandoverItemCategoryText] ?? item.category }}</span>
          <span v-if="item.batch_no" class="item-batch-no">批次 {{ item.batch_no }}</span>
          <span class="item-assignee">接手人：{{ item.assignee }}</span>
          <StatusBadge :value="item.status === 'DONE' ? '已处理' : '待处理'" />
        </div>
        <p class="item-content">{{ item.content }}</p>
        <p v-if="item.process_note" class="item-note">处理记录：{{ item.process_note }}（{{ item.processed_at ? formatDate(item.processed_at) : "" }}）</p>

        <div v-if="isClaimed" class="item-actions">
          <input
            v-model="processingNote[item.id]"
            class="item-note-input"
            type="text"
            :placeholder="item.status === 'DONE' ? '更新处理说明（可空）' : '填写处理说明（可空）'"
            :disabled="saving"
          />
          <button v-if="item.status !== 'DONE'" class="btn btn-small btn-primary" :disabled="saving" @click="toggleItem(item.id, true)">
            标记已处理
          </button>
          <button v-else class="btn btn-small btn-ghost" :disabled="saving" @click="toggleItem(item.id, false)">
            撤回为待处理
          </button>
        </div>
      </li>
    </ul>

    <!-- 处理中：全部事项办结才允许关闭，否则展示剩余条数提示 -->
    <footer v-if="isClaimed" class="handover-foot">
      <template v-if="!showCloseBox">
        <p v-if="!allDone" class="foot-hint">还有 {{ totalCount - doneCount }} 条事项未处理，全部办结后才能关闭交接单。</p>
        <button class="btn btn-primary" :disabled="saving || !allDone" :title="allDone ? '' : '仍有事项未处理'" @click="showCloseBox = true">
          全部办结，关闭交接单
        </button>
      </template>
      <template v-else>
        <input v-model="closeNote" type="text" placeholder="关闭说明（可空）" :disabled="saving" />
        <button class="btn btn-ghost" :disabled="saving" @click="showCloseBox = false">返回</button>
        <button class="btn btn-primary" :disabled="saving" @click="doClose">确认关闭</button>
      </template>
    </footer>

    <footer v-else-if="isClosed" class="handover-foot closed">
      <span>交接单已关闭，共 {{ totalCount }} 条事项全部处理完成。</span>
    </footer>
  </article>
</template>
