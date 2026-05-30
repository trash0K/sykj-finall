<template>
  <el-dialog
    :model-value="visible"
    title="补助日历"
    width="1100px"
    top="4vh"
    destroy-on-close
    class="subsidy-cal-dialog"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div class="cal-layout">
      <!-- 左侧摘要 -->
      <aside class="cal-side">
        <div class="cal-side__type">
          <span class="label">出差类型</span>
          <span class="value accent">{{ businessTypeName || '—' }}</span>
        </div>
        <div class="cal-timeline">
          <div class="tl-node">
            <span class="tl-label">开始日期</span>
            <span class="tl-date">{{ subsidy?.departureDate }}</span>
          </div>
          <div class="tl-bar">
            <div class="tl-bar__title">行程天数</div>
            <div class="tl-bar__route">{{ subsidy?.departureCity }} - {{ subsidy?.arrivingCity }}</div>
            <div class="tl-bar__days">{{ subsidy?.subsidyDays }}天</div>
          </div>
          <div class="tl-node">
            <span class="tl-label">结束日期</span>
            <span class="tl-date">{{ subsidy?.arrivalDate }}</span>
          </div>
        </div>
        <div class="cal-summary">
          <div class="sum-row">
            <span>补助金额</span>
            <span class="sum-currency">CNY</span>
            <span class="sum-val">{{ calSubsidyAmount }}</span>
          </div>
          <div class="sum-row">
            <span>标准总额</span>
            <span class="sum-currency">CNY</span>
            <span class="sum-val">{{ calStandardAmount }}</span>
          </div>
          <div class="sum-row">
            <span>补助金额</span>
            <span class="sum-currency">CNY</span>
            <span class="sum-val">{{ calSubsidyAmount }}</span>
          </div>
        </div>
      </aside>

      <!-- 右侧表格 -->
      <div class="cal-main">
        <div class="cal-main__hd">
          <span class="cal-main__title">出差补助</span>
          <el-checkbox v-model="selectAll" @change="onSelectAll">全选</el-checkbox>
        </div>
        <div class="cal-table-wrap">
          <table class="cal-table">
            <thead>
              <tr>
                <th class="col-date">出差日期</th>
                <th class="col-city">补助城市</th>
                <th class="col-subsidy">
                  <el-checkbox v-model="mealCheckAll" :indeterminate="mealIndeterminate" @change="onColAll('meal')" />
                  餐费补助
                </th>
                <th class="col-subsidy">
                  <el-checkbox v-model="transportCheckAll" :indeterminate="transportIndeterminate" @change="onColAll('transport')" />
                  交通补助
                </th>
                <th class="col-subsidy">
                  <el-checkbox v-model="phoneCheckAll" :indeterminate="phoneIndeterminate" @change="onColAll('phone')" />
                  通讯补助
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.travelDate" :class="{ 'row-active': row.checkedAll }">
                <td class="col-date">
                  <div class="date-cell">
                    <div class="date-main">
                      <span class="date-str">{{ row.travelDate }}</span>
                      <span class="week-str">{{ row.travelDateWeek }}</span>
                    </div>
                    <el-checkbox v-model="row.checkedAll" @change="onRowAll(row)" />
                    <el-icon class="pin-icon"><Location /></el-icon>
                  </div>
                </td>
                <td class="col-city">{{ row.subsidizedCities }}</td>
                <td class="col-subsidy">
                  <div class="subsidy-cell" :class="{ disabled: !row.mealChecked }">
                    <div class="rate">CNY {{ row.standardMealExpensesAmount }} / 天</div>
                    <div class="subsidy-input-row">
                      <el-checkbox v-model="row.mealChecked" @change="onCell(row)" />
                      <el-input-number
                        v-model="row.mealExpensesAmount"
                        :disabled="!row.mealChecked"
                        :min="0"
                        :max="Number(row.standardMealExpensesAmount)"
                        :precision="2"
                        :controls="false"
                        size="small"
                        @change="onAmountChange"
                      />
                    </div>
                  </div>
                </td>
                <td class="col-subsidy">
                  <div class="subsidy-cell" :class="{ disabled: !row.transportChecked }">
                    <div class="rate">CNY {{ row.standardTrafficAmount }} / 天</div>
                    <div class="subsidy-input-row">
                      <el-checkbox v-model="row.transportChecked" @change="onCell(row)" />
                      <el-input-number
                        v-model="row.trafficAmount"
                        :disabled="!row.transportChecked"
                        :min="0"
                        :max="Number(row.standardTrafficAmount)"
                        :precision="2"
                        :controls="false"
                        size="small"
                        @change="onAmountChange"
                      />
                    </div>
                  </div>
                </td>
                <td class="col-subsidy">
                  <div class="subsidy-cell" :class="{ disabled: !row.phoneChecked }">
                    <div class="rate">CNY {{ row.standardCommunicationAmount }} / 天</div>
                    <div class="subsidy-input-row">
                      <el-checkbox v-model="row.phoneChecked" @change="onCell(row)" />
                      <el-input-number
                        v-model="row.communicationAmount"
                        :disabled="!row.phoneChecked"
                        :min="0"
                        :max="Number(row.standardCommunicationAmount)"
                        :precision="2"
                        :controls="false"
                        size="small"
                        @change="onAmountChange"
                      />
                    </div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="cal-footer">
        <el-button @click="$emit('update:visible', false)">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确认</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Location } from '@element-plus/icons-vue'
import { dateRange, getWeekDay, subsidyRates, formatAmount } from '../utils/reim'

const props = defineProps({
  visible: Boolean,
  subsidy: { type: Object, default: null },
  businessTypeName: { type: String, default: '' },
  calendars: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:visible', 'confirm'])

const rows = ref([])
const selectAll = ref(false)
const mealCheckAll = ref(false)
const transportCheckAll = ref(false)
const phoneCheckAll = ref(false)
const mealIndeterminate = ref(false)
const transportIndeterminate = ref(false)
const phoneIndeterminate = ref(false)

const calSubsidyAmount = computed(() => {
  let t = 0
  rows.value.forEach(r => {
    if (r.mealChecked) t += Number(r.mealExpensesAmount) || 0
    if (r.transportChecked) t += Number(r.trafficAmount) || 0
    if (r.phoneChecked) t += Number(r.communicationAmount) || 0
  })
  return formatAmount(t)
})

const calStandardAmount = computed(() => {
  let t = 0
  rows.value.forEach(r => {
    if (r.mealChecked) t += Number(r.standardMealExpensesAmount) || 0
    if (r.transportChecked) t += Number(r.standardTrafficAmount) || 0
    if (r.phoneChecked) t += Number(r.standardCommunicationAmount) || 0
  })
  return formatAmount(t)
})

const buildRows = () => {
  const sub = props.subsidy
  if (!sub) {
    rows.value = []
    return
  }
  const dates = dateRange(sub.departureDate, sub.arrivalDate)
  const rates = subsidyRates(sub.arrivingCityNo)
  const key = sub.id || sub.travelerId + sub.departureDate + sub.arrivalDate
  const existing = props.calendars.filter(c => c.mainId === key || c.mainId === sub.id)

  rows.value = dates.map(d => {
    const ex = existing.find(c => c.travelDate === d)
    const mealChecked = ex ? Number(ex.mealExpensesAmount) > 0 : false
    const transportChecked = ex ? Number(ex.trafficAmount) > 0 : false
    const phoneChecked = ex ? Number(ex.communicationAmount) > 0 : false
    return {
      travelDate: d,
      travelDateWeek: getWeekDay(d),
      subsidizedCities: sub.arrivingCity,
      subsidizedCityNumber: sub.arrivingCityNo,
      standardMealExpensesAmount: String(rates.meal),
      standardTrafficAmount: String(rates.transport),
      standardCommunicationAmount: String(rates.communication),
      mealExpensesAmount: ex ? Number(ex.mealExpensesAmount) : rates.meal,
      trafficAmount: ex ? Number(ex.trafficAmount) : rates.transport,
      communicationAmount: ex ? Number(ex.communicationAmount) : rates.communication,
      mealChecked,
      transportChecked,
      phoneChecked,
      checkedAll: mealChecked && transportChecked && phoneChecked
    }
  })
  syncHeaderChecks()
}

watch(() => [props.visible, props.subsidy], () => {
  if (props.visible) buildRows()
}, { immediate: true })

const syncHeaderChecks = () => {
  const d = rows.value
  if (!d.length) return
  selectAll.value = d.every(r => r.checkedAll)
  const mealAll = d.every(r => r.mealChecked)
  const mealSome = d.some(r => r.mealChecked)
  mealCheckAll.value = mealAll
  mealIndeterminate.value = mealSome && !mealAll
  const tAll = d.every(r => r.transportChecked)
  const tSome = d.some(r => r.transportChecked)
  transportCheckAll.value = tAll
  transportIndeterminate.value = tSome && !tAll
  const pAll = d.every(r => r.phoneChecked)
  const pSome = d.some(r => r.phoneChecked)
  phoneCheckAll.value = pAll
  phoneIndeterminate.value = pSome && !pAll
}

const onSelectAll = (v) => {
  rows.value.forEach(r => {
    r.checkedAll = r.mealChecked = r.transportChecked = r.phoneChecked = v
    if (v) {
      r.mealExpensesAmount = Number(r.standardMealExpensesAmount)
      r.trafficAmount = Number(r.standardTrafficAmount)
      r.communicationAmount = Number(r.standardCommunicationAmount)
    } else {
      r.mealExpensesAmount = 0
      r.trafficAmount = 0
      r.communicationAmount = 0
    }
  })
  syncHeaderChecks()
}

const onRowAll = (row) => {
  row.mealChecked = row.transportChecked = row.phoneChecked = row.checkedAll
  if (row.checkedAll) {
    row.mealExpensesAmount = Number(row.standardMealExpensesAmount)
    row.trafficAmount = Number(row.standardTrafficAmount)
    row.communicationAmount = Number(row.standardCommunicationAmount)
  } else {
    row.mealExpensesAmount = 0
    row.trafficAmount = 0
    row.communicationAmount = 0
  }
  syncHeaderChecks()
}

const onColAll = (type) => {
  const v = type === 'meal' ? mealCheckAll.value : type === 'transport' ? transportCheckAll.value : phoneCheckAll.value
  rows.value.forEach(r => {
    if (type === 'meal') {
      r.mealChecked = v
      r.mealExpensesAmount = v ? Number(r.standardMealExpensesAmount) : 0
    } else if (type === 'transport') {
      r.transportChecked = v
      r.trafficAmount = v ? Number(r.standardTrafficAmount) : 0
    } else {
      r.phoneChecked = v
      r.communicationAmount = v ? Number(r.standardCommunicationAmount) : 0
    }
    r.checkedAll = r.mealChecked && r.transportChecked && r.phoneChecked
  })
  syncHeaderChecks()
}

const onCell = (row) => {
  if (!row.mealChecked) row.mealExpensesAmount = 0
  else if (!row.mealExpensesAmount) row.mealExpensesAmount = Number(row.standardMealExpensesAmount)
  if (!row.transportChecked) row.trafficAmount = 0
  else if (!row.trafficAmount) row.trafficAmount = Number(row.standardTrafficAmount)
  if (!row.phoneChecked) row.communicationAmount = 0
  else if (!row.communicationAmount) row.communicationAmount = Number(row.standardCommunicationAmount)
  row.checkedAll = row.mealChecked && row.transportChecked && row.phoneChecked
  syncHeaderChecks()
}

const onAmountChange = () => syncHeaderChecks()

const handleConfirm = () => {
  emit('confirm', rows.value.map(r => ({ ...r })))
}
</script>

<style scoped>
.cal-layout {
  display: flex;
  gap: 0;
  min-height: 420px;
  border: 1px solid #ebeef5;
}
.cal-side {
  width: 220px;
  flex-shrink: 0;
  border-right: 1px solid #ebeef5;
  padding: 16px 14px;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}
.cal-side__type {
  margin-bottom: 20px;
}
.cal-side__type .label {
  display: block;
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
}
.cal-side__type .value.accent {
  color: #e6a23c;
  font-weight: bold;
  font-size: 14px;
}
.cal-timeline {
  flex: 1;
  position: relative;
  padding-left: 8px;
}
.cal-timeline::before {
  content: '';
  position: absolute;
  left: 3px;
  top: 12px;
  bottom: 12px;
  width: 2px;
  background: #409eff;
}
.tl-node {
  position: relative;
  padding-left: 16px;
  margin-bottom: 8px;
}
.tl-node::before {
  content: '';
  position: absolute;
  left: -2px;
  top: 6px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  border: 2px solid #fff;
}
.tl-label {
  display: block;
  font-size: 12px;
  color: #909399;
}
.tl-date {
  font-size: 14px;
  color: #303133;
}
.tl-bar {
  margin: 10px 0 10px 16px;
  background: #409eff;
  color: #fff;
  padding: 8px 10px;
  border-radius: 2px;
  font-size: 12px;
}
.tl-bar__title {
  opacity: 0.9;
  margin-bottom: 4px;
}
.tl-bar__route {
  font-weight: bold;
}
.tl-bar__days {
  text-align: right;
  margin-top: 4px;
}
.cal-summary {
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}
.sum-row {
  display: flex;
  align-items: baseline;
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  gap: 6px;
}
.sum-currency {
  margin-left: auto;
  font-size: 12px;
}
.sum-val {
  color: #e6a23c;
  font-weight: bold;
  min-width: 56px;
  text-align: right;
}
.cal-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.cal-main__hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}
.cal-main__title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
}
.cal-table-wrap {
  overflow: auto;
  flex: 1;
}
.cal-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.cal-table th,
.cal-table td {
  border: 1px solid #ebeef5;
  padding: 10px 8px;
  vertical-align: top;
}
.cal-table th {
  background: #f5f7fa;
  font-weight: normal;
  color: #606266;
  text-align: center;
}
.col-date { width: 140px; }
.col-city { width: 90px; text-align: center; }
.col-subsidy { min-width: 160px; }
.date-cell {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}
.date-main {
  flex: 1;
}
.date-str {
  display: block;
  color: #303133;
}
.week-str {
  display: block;
  color: #909399;
  font-size: 12px;
}
.pin-icon {
  color: #c0c4cc;
  font-size: 14px;
}
.rate {
  color: #e6a23c;
  font-size: 12px;
  margin-bottom: 6px;
}
.subsidy-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.subsidy-cell.disabled .rate {
  opacity: 0.5;
}
.subsidy-cell :deep(.el-input-number) {
  width: 90px;
}
.subsidy-cell :deep(.el-input__wrapper) {
  background: #f5f7fa;
}
.subsidy-cell:not(.disabled) :deep(.el-input__wrapper) {
  background: #fff;
}
.cal-footer {
  text-align: center;
  width: 100%;
}
:deep(.subsidy-cal-dialog .el-dialog__body) {
  padding: 0;
}
:deep(.subsidy-cal-dialog .el-dialog__footer) {
  text-align: center;
  border-top: 1px solid #ebeef5;
}
</style>
