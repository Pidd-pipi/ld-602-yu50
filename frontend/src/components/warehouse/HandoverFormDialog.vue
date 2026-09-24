<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import type { Warehouse } from "../../types/Warehouse";
import type { HandoverSheet, HandoverCreatePayload } from "../../types/HandoverSheet";
import type { HandoverItemInput } from "../../types/HandoverItem";
import {
  createDefaultHandoverForm,
  createDefaultHandoverItemInput,
  createHandoverItemTypeOptions,
  toIso
} from "../../constructors/ShiftHandoverConstructor";
import { useShiftHandover } from "../../hooks/useShiftHandover";
import { useShiftHandoverStore } from "../../stores/ShiftHandoverStore";
import { HandoverApiError } from "../../api/HandoverApiError";

const props = defineProps<{ open: boolean; warehouses: Warehouse[]; warehouseId: number; existing: HandoverSheet[] }>();
const emit = defineEmits<{
  (e: "update:open", value: boolean): void;
  (e: "submitted", sheet: HandoverSheet): void;
}>();

const store = useShiftHandoverStore();
const { findOverlap, validateWindow } = useShiftHandover();
const typeOptions = createHandoverItemTypeOptions();

const buildForm = (warehouseId: number) => createDefaultHandoverForm(warehouseId);
const form = reactive(buildForm(props.warehouseId));
const errorMessage = ref("");
const submitting = ref(false);

const close = () => {
  errorMessage.value = "";
  emit("update:open", false);
};

const addItem = () => form.items.push(createDefaultHandoverItemInput());

// 每次打开弹窗都按当前选中仓库重置表单与默认时段
watch(
  () => props.open,
  (open) => {
    if (!open) return;
    Object.assign(form, buildForm(props.warehouseId));
    form.items = [createDefaultHandoverItemInput()];
    errorMessage.value = "";
  }
);
const removeItem = (index: number) => {
  if (form.items.length === 1) return;
  form.items.splice(index, 1);
};

const submit = async () => {
  errorMessage.value = "";
  if (!form.manager_name.trim()) {
    errorMessage.value = "请填写当班负责人";
    return;
  }
  const windowError = validateWindow(form.shift_start, form.shift_end);
  if (windowError) {
    errorMessage.value = windowError;
    return;
  }
  const items: HandoverItemInput[] = form.items.map((item) => ({
    type: item.type,
    content: item.content.trim(),
    assignee: item.assignee.trim()
  }));
  const invalidIndex = items.findIndex((item) => !item.content || !item.assignee);
  if (invalidIndex >= 0) {
    errorMessage.value = `第 ${invalidIndex + 1} 条事项必须填写事项内容和接手人`;
    return;
  }
  const conflict = findOverlap(props.existing, form.warehouse_id, toIso(form.shift_start), toIso(form.shift_end));
  if (conflict) {
    errorMessage.value = `该仓库所选时段与有效交接单重叠，冲突单：HO-${String(conflict.id).padStart(4, "0")}`;
    return;
  }

  const payload: HandoverCreatePayload = {
    warehouse_id: form.warehouse_id,
    manager_name: form.manager_name.trim(),
    shift_start: toIso(form.shift_start),
    shift_end: toIso(form.shift_end),
    items
  };
  submitting.value = true;
  try {
    const saved = await store.create(payload);
    emit("submitted", saved);
    close();
  } catch (error) {
    errorMessage.value = error instanceof HandoverApiError ? error.message : "创建失败，请稍后重试";
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <div v-if="open" class="modal-mask" @click.self="close">
    <div class="modal handover-form" role="dialog" aria-modal="true" aria-label="新建值班交接单">
      <header class="modal-head">
        <h3>新建值班交接单</h3>
        <button class="icon-btn" type="button" @click="close">×</button>
      </header>
      <div class="form-grid">
        <label class="field span-2">
          <span>交接仓库</span>
          <select v-model.number="form.warehouse_id">
            <option v-for="warehouse in warehouses" :key="warehouse.id" :value="warehouse.id">{{ warehouse.name }}</option>
          </select>
        </label>
        <label class="field">
          <span>当班负责人</span>
          <input v-model="form.manager_name" maxlength="20" placeholder="如：周敏（交班）" />
        </label>
        <label class="field">
          <span>时段开始</span>
          <input v-model="form.shift_start" type="datetime-local" />
        </label>
        <label class="field">
          <span>时段结束</span>
          <input v-model="form.shift_end" type="datetime-local" />
        </label>
      </div>

      <div class="item-editor">
        <div class="item-editor-head">
          <strong>交接事项（逐条登记，均需指定接手人）</strong>
          <button type="button" class="text-btn" @click="addItem">+ 添加事项</button>
        </div>
        <div v-for="(item, index) in form.items" :key="index" class="item-row">
          <select v-model="item.type" aria-label="事项类型">
            <option v-for="option in typeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
          <input v-model="item.content" class="item-content" placeholder="事项说明，如到货批次/临期批次号/异常情况" />
          <input v-model="item.assignee" class="item-assignee" placeholder="接手人" maxlength="20" />
          <button type="button" class="icon-btn danger" :disabled="form.items.length === 1" @click="removeItem(index)">×</button>
        </div>
      </div>

      <p v-if="errorMessage" class="form-error">⚠ {{ errorMessage }}</p>
      <footer class="modal-foot">
        <button type="button" class="btn ghost" @click="close">取消</button>
        <button type="button" class="btn primary" :disabled="submitting" @click="submit">
          {{ submitting ? "提交中…" : "登记交接单" }}
        </button>
      </footer>
    </div>
  </div>
</template>
