<template>
  <!-- 툴팁만 사용하는 경우 -->
  <v-tooltip v-if="!useDialog" location="top">
    <template v-slot:activator="{ props }">
      <v-btn 
        v-bind="props"
        icon 
        :size="size" 
        variant="text" 
        :color="color"
        class="ml-1"
      >
        <v-icon :size="iconSize">mdi-help-circle-outline</v-icon>
      </v-btn>
    </template>
    <span>{{ tooltip }}</span>
  </v-tooltip>

  <!-- 다이얼로그 사용하는 경우 -->
  <template v-else>
    <v-btn 
      icon 
      :size="size" 
      variant="text" 
      :color="color"
      class="ml-1"
      @click="showDialog = true"
    >
      <v-icon :size="iconSize">mdi-help-circle-outline</v-icon>
    </v-btn>

    <v-dialog v-model="showDialog" :max-width="dialogWidth">
      <v-card>
        <v-card-title class="bg-indigo-darken-2 text-white d-flex align-center">
          <v-icon class="mr-2">mdi-help-circle</v-icon>
          {{ dialogTitle }}
          <v-spacer />
          <v-btn icon variant="text" color="white" @click="showDialog = false">
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </v-card-title>
        <v-card-text class="pa-4">
          <div v-html="dialogContent" class="help-content"></div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn color="primary" variant="flat" @click="showDialog = false">확인</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </template>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  tooltip?: string
  useDialog?: boolean
  dialogTitle?: string
  dialogContent?: string
  dialogWidth?: number | string
  size?: string
  iconSize?: number | string
  color?: string
}

const props = withDefaults(defineProps<Props>(), {
  tooltip: '',
  useDialog: false,
  dialogTitle: '도움말',
  dialogContent: '',
  dialogWidth: 700,
  size: 'x-small',
  iconSize: 14,
  color: 'white'
})

const showDialog = ref(false)
</script>

<style scoped>
.help-content {
  line-height: 1.6;
}

.help-content :deep(p) {
  margin-bottom: 12px;
}

.help-content :deep(p:last-child) {
  margin-bottom: 0;
}

.help-content :deep(strong) {
  color: #1565C0;
}

.help-content :deep(.help-intro) {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e0e0e0;
  color: #424242;
  font-size: 14px;
}

.help-content :deep(.help-item) {
  margin-bottom: 16px;
  padding-left: 8px;
}

.help-content :deep(.help-bullet) {
  color: #1565C0;
  font-weight: bold;
  margin-right: 6px;
}

.help-content :deep(.help-desc) {
  display: block;
  padding-left: 20px;
  margin-top: 4px;
  color: #616161;
  font-size: 13px;
}

.help-content :deep(.help-note) {
  margin-top: 16px;
  padding: 10px 12px;
  background-color: #FFF8E1;
  border-left: 3px solid #FFA000;
  border-radius: 4px;
  color: #5D4037;
  font-size: 13px;
}
</style>