<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { HandoverItemCategory, HandoverItemCategoryText } from "../../constants/HandoverItemCategory";
import { createHandoverItemForm, createShiftHandoverForm } from "../../constructors/ShiftHandoverConstructor";
import type { ShiftHandover } from "../../types/ShiftHandover";
import type { Warehouse } from "../../types/Warehouse";

const props = defineProps<{
  warehouses: Warehouse[];
  warehouseId: number;
  saving: boolean;
  /** 当前仓库的有效交接单：仅当选定时段与其重叠才阻止新建，非重叠时段允许登记。 */
  activeHandovers: ShiftHandover[];
}>();

const emit = defineEmits<{
  (e: "submit", payload: ReturnType<typeof createShiftHandoverForm>): void;
  (e: "cancel"): void;
}>();

const form = reactive(createShiftHandoverForm({ warehouse_id: props.warehouseId }));
const formError = ref("");

const categoryOptions = HandoverItemCategory.map((value) => ({
  value,
  label: HandoverItemCategoryText[value]
}));

const toTime = (value: string) => {
  if (!value) return Number.NaN;
  const normalized = value.length === 16 ? `${value}:00` : value;
  return new Date(normalized.replace(" ", "T")).getTime();
};

/** 选中仓库在所选时段内是否已存在有效交接单（start < other.end && end > other.start）。 */
const overlap = computed(() => {
  const start = toTime(form.shift_start_at);
  const end = toTime(form.shift_end_at);
  if (Number.isNaN(start) || Number.isNaN(end)) return null;
  return (
    props.activeHandovers
      .filter((handover) => handover.warehouse_id === form.warehouse_id)
      .find((handover) => {
        const otherStart = toTime(handover.shift_start_at);
        const otherEnd = toTime(handover.shift_end_at);
        return start < otherEnd && end > otherStart;
      }) ?? null
  );
});

function addItem() {
  form.items.push(createHandoverItemForm());
}

function removeItem(index: number) {
  if (form.items.length === 1) return;
  form.items.splice(index, 1);
}

function submit() {
  formError.value = "";
  if (!form.warehouse_id) {
    formError.value = "请选择交接仓库。";
    return;
  }
  if (!form.shift_owner.trim()) {
    formError.value = "请选择当班负责人。";
    return;
  }
  if (!form.shift_start_at || !form.shift_end_at) {
    formError.value = "请选择完整的值守时段。";
    return;
  }
  if (form.shift_end_at <= form.shift_start_at) {
    formError.value = "值守结束时间必须晚于开始时间。";
    return;
  }
  if (overlap.value) {
    formError.value = `与有效交接单 #${overlap.value.id}（${overlap.value.shift_start_at} ~ ${overlap.value.shift_end_at}）时段重叠，同一仓库的重叠时段只能有一张有效交接单，原单与库存保持不变。`;
    return;
  }
  if (form.items.length === 0) {
    formError.value = "至少登记一条交接事项。";
    return;
  }
  for (const [index, item] of form.items.entries()) {
    if (!item.content.trim()) {
      formError.value = `第 ${index + 1} 条事项缺少内容描述。`;
      return;
    }
    if (!item.assignee.trim()) {
      formError.value = `第 ${index + 1} 条事项未指定接手人。`;
      return;
    }
  }
  emit("submit", { ...form, items: form.items.map((item) => ({ ...item })) });
}
</script>

<template>
  <form class="panel handover-form" @submit.prevent="submit">
    <h2>新建值班交接单</h2>
    <p v-if="activeHandovers.length" class="form-warn">
      该仓库现有 {{ activeHandovers.length }} 张有效交接单；请选择不与其重叠的值守时段，冲突时系统会拒绝并保留原单与库存。
    </p>
    <div class="form-grid">
      <label class="field span-2">
        <span>交接仓库</span>
        <select v-model.number="form.warehouse_id">
          <option v-for="warehouse in props.warehouses" :key="warehouse.id" :value="warehouse.id">
            {{ warehouse.name }}（{{ warehouse.district }}）
          </option>
        </select>
      </label>
      <label class="field">
        <span>当班负责人</span>
        <input v-model="form.shift_owner" type="text" list="shift-owner-options" placeholder="如：周敏（白班）" />
        <datalist id="shift-owner-options">
          <option v-for="warehouse in props.warehouses" :key="`owner-${warehouse.id}`" :value="`仓管员 #${warehouse.manager_id}`" />
          <option value="周敏（白班）" />
          <option value="孙立军（夜班）" />
        </datalist>
      </label>
      <label class="field field-inline"></label>
      <label class="field">
        <span>值守开始</span>
        <input v-model="form.shift_start_at" type="datetime-local" />
      </label>
      <label class="field">
        <span>值守结束</span>
        <input v-model="form.shift_end_at" type="datetime-local" />
      </label>
      <p v-if="overlap" class="overlap-warn">
        所选时段与交接单 #{{ overlap.id }} 重叠，不能登记；原单与库存不会改动。
      </p>
      <label class="field span-2">
        <span>值班备注</span>
        <input v-model="form.remark" type="text" placeholder="本班次总体情况、重点提醒（可空）" />
      </label>
    </div>

    <div class="items-head">
      <h3>交接事项（逐条登记事项与接手人）</h3>
      <button type="button" class="btn btn-ghost" @click="addItem">+ 增加事项</button>
    </div>
    <div class="item-editor" v-for="(item, index) in form.items" :key="index">
      <div class="item-editor-row">
        <select v-model="item.category" class="item-category">
          <option v-for="option in categoryOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
        <input v-model="item.batch_no" class="item-batch" type="text" placeholder="关联批次号（可空）" />
        <input v-model="item.assignee" class="item-assignee" type="text" placeholder="接手人 *" />
        <button type="button" class="btn btn-danger-ghost" :disabled="form.items.length === 1" @click="removeItem(index)">删除</button>
      </div>
      <textarea v-model="item.content" rows="2" placeholder="事项内容：待入库单号/临期批次/库存异常说明 *"></textarea>
    </div>

    <p v-if="formError" class="form-error">{{ formError }}</p>
    <div class="form-actions">
      <button type="button" class="btn btn-ghost" @click="emit('cancel')">取消</button>
      <button type="submit" class="btn btn-primary" :disabled="saving || !!overlap">
        {{ saving ? "提交中…" : "登记交接单" }}
      </button>
    </div>
  </form>
</template>
