<script setup lang="ts">
withDefaults(defineProps<{ value: string; label?: string; tone?: "ok" | "warn" | "info" | "muted" }>(), {
  label: undefined,
  tone: "ok"
});
</script>
<template><span class="badge" :data-tone="tone">{{ label ?? value.replace(/_/g, " ") }}</span></template>
