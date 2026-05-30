<template>
  <el-dialog
    :model-value="visible"
    title="提示"
    width="420px"
    align-center
    destroy-on-close
    @update:model-value="$emit('update:visible', $event)"
  >
    <div class="confirm-body">
      <el-icon class="confirm-icon" :size="22"><WarningFilled /></el-icon>
      <span>{{ message }}</span>
    </div>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="$emit('confirm')">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { WarningFilled } from '@element-plus/icons-vue'

defineProps({
  visible: Boolean,
  message: { type: String, default: '确认删除?' }
})
defineEmits(['update:visible', 'confirm'])
</script>

<style scoped>
.confirm-body {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px;
  font-size: 14px;
  color: #303133;
}
.confirm-icon {
  color: #e6a23c;
  flex-shrink: 0;
}
</style>
