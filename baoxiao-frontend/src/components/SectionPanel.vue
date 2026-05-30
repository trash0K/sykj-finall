<template>
  <div class="section-panel">
    <div class="section-panel__hd" @click="collapsible ? toggle() : null">
      <span class="section-panel__bar" />
      <span class="section-panel__title">
        <slot name="title">{{ title }}</slot>
      </span>
      <div class="section-panel__extra" @click.stop>
        <slot name="extra" />
      </div>
      <el-icon v-if="collapsible" class="section-panel__arrow">
        <ArrowUp v-if="isExpanded" />
        <ArrowDown v-else />
      </el-icon>
    </div>
    <div v-show="!collapsible || isExpanded" class="section-panel__bd">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, default: '' },
  collapsible: { type: Boolean, default: true },
  defaultExpanded: { type: Boolean, default: true },
  expanded: { type: Boolean, default: undefined }
})

const emit = defineEmits(['update:expanded'])

const innerExpanded = ref(props.expanded ?? props.defaultExpanded)

watch(() => props.expanded, v => {
  if (v !== undefined) innerExpanded.value = v
})

const isExpanded = computed({
  get: () => (props.expanded !== undefined ? props.expanded : innerExpanded.value),
  set: (v) => {
    innerExpanded.value = v
    emit('update:expanded', v)
  }
})

const toggle = () => { isExpanded.value = !isExpanded.value }
defineExpose({ expanded: isExpanded, toggle })
</script>

<style scoped>
.section-panel {
  border-bottom: 1px solid #ebeef5;
}
.section-panel__hd {
  height: 36px;
  display: flex;
  align-items: center;
  padding: 0 16px 0 0;
  background: #f5f7fa;
  cursor: default;
  user-select: none;
}
.section-panel__hd:has(.section-panel__arrow) {
  cursor: pointer;
}
.section-panel__bar {
  width: 4px;
  height: 18px;
  background: #409eff;
  margin-right: 10px;
  flex-shrink: 0;
}
.section-panel__title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  flex: 1;
  min-width: 0;
}
.section-panel__extra {
  margin-right: 12px;
}
.section-panel__arrow {
  font-size: 14px;
  color: #909399;
}
.section-panel__bd {
  padding: 16px 20px;
  font-size: 14px;
}
</style>
