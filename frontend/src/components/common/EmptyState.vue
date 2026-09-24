<script setup lang="ts">withDefaults(defineProps<{ text?: string }>(), { text: "暂无数据" });</script>
<template><div class="empty"><span>{{ text }}</span></div></template>
