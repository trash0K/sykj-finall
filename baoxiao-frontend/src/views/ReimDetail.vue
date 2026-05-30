<template>
  <div class="detail-root">
    <div class="detail-wrap">
      <header class="doc-header">
        <h1 class="doc-title">差旅费用报销单</h1>
        <span class="doc-date">填单日期 {{ displayDate }}</span>
      </header>

      <div class="detail-body">
        <!-- 基本信息 -->
        <SectionPanel v-model:expanded="expanded.basic" title="基本信息">
          <el-form label-width="110px" label-position="right" class="basic-form">
            <el-form-item label="报销标题" required>
              <el-input v-model="form.main.reimbursementTitle" :disabled="readonly" maxlength="500" show-word-limit placeholder="请输入" />
            </el-form-item>
            <el-row :gutter="24">
              <el-col :span="8">
                <el-form-item label="报销人" required>
                  <el-select v-model="reimbSel" :disabled="readonly" placeholder="请选择" style="width:100%" @change="onReimbChange">
                    <el-option v-for="e in employees" :key="e.reimburserId" :label="e.reimburserName" :value="e.reimburserId" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="报销部门" required>
                  <el-select v-model="deptSel" :disabled="readonly" placeholder="请选择" style="width:100%" @change="onDeptChange">
                    <el-option v-for="d in departments" :key="d.reimDepartmentId" :label="d.reimDepartmentName" :value="d.reimDepartmentId" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="费用归属公司" required>
                  <el-select v-model="compSel" :disabled="readonly" placeholder="请选择" style="width:100%" @change="onCompChange">
                    <el-option v-for="c in companies" :key="c.reimCompanyId" :label="c.reimCompanyName" :value="c.reimCompanyId" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="24">
              <el-col :span="8">
                <el-form-item label="业务类型" required>
                  <el-tree-select
                    v-model="form.main.businessTypeId"
                    :data="bizTree"
                    :props="{ label: 'businessTypeName', value: 'businessTypeId', children: 'children' }"
                    :disabled="readonly"
                    placeholder="请选择"
                    check-strictly
                    style="width:100%"
                    @change="onBizChange"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="16">
                <el-form-item label="出差事由" required>
                  <el-input v-model="form.main.businessTripReason" :disabled="readonly" maxlength="500" show-word-limit placeholder="请输入" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </SectionPanel>

        <!-- 补录行程 -->
        <SectionPanel v-model:expanded="expanded.itinerary" :collapsible="true">
          <template #title>补录行程</template>
          <template #extra>
            <el-button v-if="!readonly" link type="primary" class="hd-link" @click.stop="openItinDlg(null)">
              <el-icon><Plus /></el-icon> 补录行程
            </el-button>
          </template>
          <el-table :data="form.itineraries" border size="small" class="data-table">
            <el-table-column type="index" label="序号" width="55" align="center" />
            <el-table-column label="出行人员" width="140">
              <template #default="{ row }">{{ row.travelerName }}/{{ row.travelerNo }}</template>
            </el-table-column>
            <el-table-column label="出差日期" width="220">
              <template #default="{ row }">{{ row.departureDate }} 至 {{ row.arrivalDate }}</template>
            </el-table-column>
            <el-table-column label="行程" min-width="120">
              <template #default="{ row }">{{ row.departureCity }} - {{ row.arrivingCity }}</template>
            </el-table-column>
            <el-table-column prop="itineraryInstructions" label="行程说明" min-width="160" show-overflow-tooltip />
            <el-table-column v-if="!readonly" label="操作" width="120" align="center">
              <template #default="{ row, $index }">
                <el-button link type="primary" @click="prepDelItin($index)"><el-icon><Delete /></el-icon></el-button>
                <el-button link type="primary" @click="openItinDlg(row)"><el-icon><Edit /></el-icon></el-button>
                <el-button link type="primary" @click="copyItin(row)"><el-icon><CopyDocument /></el-icon></el-button>
              </template>
            </el-table-column>
          </el-table>
        </SectionPanel>

        <!-- 补助信息 -->
        <SectionPanel v-model:expanded="expanded.subsidy" :collapsible="true">
          <template #title>
            补助信息 <span class="hd-amount">{{ cost.subsidyTotal }}</span>
            <span v-if="subsidySummary" class="hd-sub">({{ subsidySummary }})</span>
          </template>
          <div class="subsidy-warn">
            <el-icon class="warn-icon"><WarningFilled /></el-icon>
            <el-tooltip placement="top" :show-after="300">
              <template #content>
                1、请根据实际出差日期选择补助 2、出差期间当日有用餐安排的请自行核减当日餐补 3、出差期间当日有用车的，请自行核减当日交补
              </template>
              <span class="warn-text">1、请根据实际出差日期选择补助 2、出差期间当日有用餐安排的请自行核减当日餐补 3、出差期间当日有用车的，请自行核减当日交补</span>
            </el-tooltip>
          </div>
          <el-table :data="form.subsidies" border size="small" class="data-table">
            <el-table-column type="index" label="序号" width="55" align="center" />
            <el-table-column prop="travelerName" label="出行人" width="90" />
            <el-table-column label="出差日期" width="220">
              <template #default="{ row }">{{ row.departureDate }} 至 {{ row.arrivalDate }}</template>
            </el-table-column>
            <el-table-column prop="subsidyDays" label="补助天数" width="80" align="center" />
            <el-table-column label="行程" min-width="130">
              <template #default="{ row }">{{ row.departureCity }}-{{ row.arrivingCity }}</template>
            </el-table-column>
            <el-table-column prop="arrivingCity" label="补助城市" width="90" />
            <el-table-column prop="applicationAmount" label="申请金额" width="100" align="right" />
            <el-table-column prop="subsidyAmount" label="补助金额" width="100" align="right" />
            <el-table-column v-if="!readonly" label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button link type="primary" @click="openSubDlg($index)"><el-icon><Edit /></el-icon></el-button>
              </template>
            </el-table-column>
          </el-table>
        </SectionPanel>

        <!-- 费用合计 -->
        <SectionPanel v-model:expanded="expanded.cost" title="费用合计" :collapsible="true">
          <div class="cost-row">
            <div class="cost-item">
              <span class="cost-label">补助总金额</span>
              <span class="cost-val">{{ cost.subsidyTotal }}</span>
            </div>
            <div class="cost-item">
              <span class="cost-label">餐费补助</span>
              <span class="cost-val">{{ cost.mealTotal }}</span>
            </div>
            <div class="cost-item">
              <span class="cost-label">交通补助</span>
              <span class="cost-val">{{ cost.transportTotal }}</span>
            </div>
            <div class="cost-item">
              <span class="cost-label">通讯补助</span>
              <span class="cost-val">{{ cost.phoneTotal }}</span>
            </div>
          </div>
        </SectionPanel>

        <!-- 费用归属及分摊 -->
        <SectionPanel v-model:expanded="expanded.alloc" :collapsible="true">
          <template #title>
            费用归属及分摊 <span class="hd-sub">(分摊金额: {{ cost.subsidyTotal }})</span>
          </template>
          <el-table :data="form.allocations" border size="small" class="data-table alloc-table">
            <el-table-column type="index" label="序号" width="55" align="center" />
            <el-table-column min-width="200">
              <template #header><span class="req">费用归属</span></template>
              <template #default="{ row }">
                <el-select v-model="row.reimCompanyId" :disabled="readonly" placeholder="请选择" filterable clearable style="width:100%" @change="v => onAllocCompanyChange(v, row)">
                  <el-option v-for="c in companies" :key="c.reimCompanyId" :label="c.reimCompanyName" :value="c.reimCompanyId" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="项目" min-width="180">
              <template #default="{ row }">
                <el-select v-model="row.projectId" :disabled="readonly" placeholder="请选择" clearable style="width:100%" @change="v => onProjChange(v, row)">
                  <el-option v-for="p in projects" :key="p.projectId" :label="p.projectName" :value="p.projectId" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column width="150" align="right">
              <template #header>
                <div class="ratio-hd">
                  <el-button v-if="!readonly" link type="primary" class="even-btn" @click="evenAlloc">
                    <el-icon><Refresh /></el-icon> 均摊
                  </el-button>
                  <span class="req">分摊比例</span>
                </div>
              </template>
              <template #default="{ row, $index }">
                <span v-if="$index === 0" class="ratio-readonly">{{ toPercent(row.allocationRatio) }} %</span>
                <el-input
                  v-else
                  v-model="row._ratioPercent"
                  :disabled="readonly"
                  class="ratio-input"
                  @blur="onRatioBlur(row)"
                  @change="onRatioChange"
                >
                  <template #suffix>%</template>
                </el-input>
              </template>
            </el-table-column>
            <el-table-column width="130" align="right">
              <template #header><span class="req">分摊金额</span></template>
              <template #default="{ row, $index }">
                <span v-if="$index === 0" class="amount-readonly">{{ row.allocationAmount || '0.00' }}</span>
                <el-input v-else v-model="row.allocationAmount" :disabled="readonly" class="amount-input" @change="onAmountRowChange(row)" />
              </template>
            </el-table-column>
            <el-table-column v-if="!readonly" label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button link type="primary" @click="prepDelAlloc($index)"><el-icon><Delete /></el-icon></el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!readonly" class="add-row-link" @click="addAlloc">
            <el-icon><CirclePlus /></el-icon> 添加一行
          </div>
          <div class="alloc-footer">
            <span class="footer-label">合计</span>
            <span class="footer-ratio">{{ allocTotalRatio }}%</span>
            <span class="footer-amt">CNY {{ allocTotalAmount }}</span>
          </div>
        </SectionPanel>

        <!-- 备注信息 -->
        <SectionPanel v-model:expanded="expanded.remark" :collapsible="true">
          <template #title>备注信息</template>
          <template #extra>
            <el-button v-if="!readonly" link type="primary" class="hd-link" @click.stop="delRemarkDlg = true">
              <el-icon><Delete /></el-icon> 删除备注
            </el-button>
          </template>
          <el-input v-model="form.main.remarks" :disabled="readonly" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="请输入" />
        </SectionPanel>
      </div>

      <footer class="doc-footer">
        <el-button plain type="primary" @click="closeDlg = true">关闭</el-button>
        <el-button v-if="!readonly" type="primary" @click="doSave">保存</el-button>
      </footer>
    </div>

    <ItineraryDialog
      v-model:visible="itinDlg.visible"
      :edit-row="itinDlg.edit"
      :employees="employees"
      :cities="cities"
      @save="saveItin"
    />

    <SubsidyCalendarDialog
      v-model:visible="subDlg.visible"
      :subsidy="subDlg.data"
      :business-type-name="form.main.businessTypeName"
      :calendars="form.calendars"
      @confirm="saveSubCalendar"
    />

    <ConfirmDialog v-model:visible="delItinDlg.visible" message="确认删除?" @confirm="confirmDelItin" />
    <ConfirmDialog v-model:visible="delAllocDlg.visible" message="确认删除?" @confirm="confirmDelAlloc" />
    <ConfirmDialog v-model:visible="delRemarkDlg" message="确认删除?" @confirm="confirmDelRemark" />
    <ConfirmDialog v-model:visible="closeDlg" message="确定要关闭当前页面吗？未保存的数据将会丢失。" @confirm="confirmClose" />

    <el-dialog v-model="saveOkDlg" title="提示" width="420px" align-center>
      <div class="confirm-body">
        <el-icon class="confirm-icon" :size="22"><SuccessFilled /></el-icon>
        <span>保存成功</span>
      </div>
      <template #footer>
        <el-button type="primary" @click="confirmSaveOk">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Plus, Delete, Edit, CopyDocument, WarningFilled, Refresh,
  CirclePlus, SuccessFilled
} from '@element-plus/icons-vue'
import { reimApi, dictApi } from '../api'
import {
  buildTree, findTreeNode, daysBetween, subsidyRates,
  toPercent, todayStr, formatAmount
} from '../utils/reim'
import SectionPanel from '../components/SectionPanel.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import ItineraryDialog from '../components/ItineraryDialog.vue'
import SubsidyCalendarDialog from '../components/SubsidyCalendarDialog.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const editId = computed(() => route.params.id)
const readonly = computed(() => route.query.mode === 'view')

const expanded = reactive({
  basic: true,
  itinerary: true,
  subsidy: true,
  cost: true,
  alloc: true,
  remark: true
})

const form = reactive({
  main: {
    id: '',
    reimbursementTitle: '',
    businessTripReason: '',
    reimburserId: '',
    reimburserNo: '',
    reimburserName: '',
    reimDepartmentId: '',
    reimDepartmentNo: '',
    reimDepartmentName: '',
    reimCompanyId: '',
    reimCompanyNo: '',
    reimCompanyName: '',
    businessTypeId: '',
    businessTypeNo: '',
    businessTypeName: '',
    subsidyTotal: '0.00',
    mealAllowance: '0.00',
    transportationAllowance: '0.00',
    phoneAllowance: '0.00',
    remarks: '',
    creationTime: ''
  },
  itineraries: [],
  subsidies: [],
  calendars: [],
  allocations: []
})

const reimbSel = ref('')
const deptSel = ref('')
const compSel = ref('')

const companies = ref([])
const departments = ref([])
const employees = ref([])
const bizTree = ref([])
const cities = ref([])
const projects = ref([])

const displayDate = computed(() => form.main.creationTime || todayStr())

const cost = computed(() => {
  let s = 0, m = 0, t = 0, p = 0
  form.subsidies.forEach(x => {
    s += +x.subsidyAmount || 0
    m += +x.mealAllowance || 0
    t += +x.transportationAllowance || 0
    p += +x.phoneAllowance || 0
  })
  return {
    subsidyTotal: formatAmount(s),
    mealTotal: formatAmount(m),
    transportTotal: formatAmount(t),
    phoneTotal: formatAmount(p)
  }
})

const subsidySummary = computed(() => {
  if (!form.subsidies.length) return ''
  const names = [...new Set(form.subsidies.map(s => `${s.travelerName}:${s.subsidyDays}天`))]
  return names.join('、')
})

const allocTotalRatio = computed(() => {
  const sum = form.allocations.reduce((s, a) => s + (Number(a.allocationRatio) || 0), 0)
  return (sum * 100).toFixed(2)
})

const allocTotalAmount = computed(() => {
  const sum = form.allocations.reduce((s, a) => s + (Number(a.allocationAmount) || 0), 0)
  return formatAmount(sum)
})

// —— 补录行程 ——
const itinDlg = reactive({ visible: false, edit: null })

const itineraryKey = (row) => row.travelerId + row.departureDate + row.arrivalDate

const removeSubsidyByItinerary = (itin) => {
  const key = itineraryKey(itin)
  const removedIds = new Set(
    form.subsidies
      .filter(s => itineraryKey(s) === key)
      .map(s => s.id)
      .filter(Boolean)
  )
  form.subsidies = form.subsidies.filter(s => itineraryKey(s) !== key)
  form.calendars = form.calendars.filter(c => !removedIds.has(c.mainId) && c.mainId !== key)
}

const openItinDlg = (row) => {
  itinDlg.edit = row
  itinDlg.visible = true
}

const copyItin = (row) => {
  itinDlg.edit = { ...row, id: '' }
  itinDlg.visible = true
}

const saveItin = (data) => {
  const overlap = form.itineraries.some(r => {
    if (itinDlg.edit && itinDlg.edit === r) return false
    return r.travelerId === data.travelerId &&
      data.departureDate <= r.arrivalDate &&
      data.arrivalDate >= r.departureDate
  })
  if (overlap) {
    ElMessage.warning('该出行人已有重叠的行程日期')
    return
  }
  const entry = {
    id: itinDlg.edit?.id || `it${Date.now()}`,
    mainId: form.main.id || '',
    ...data
  }
  if (itinDlg.edit?.id) {
    removeSubsidyByItinerary(itinDlg.edit)
    const idx = form.itineraries.findIndex(r => r.id === itinDlg.edit.id)
    if (idx >= 0) form.itineraries[idx] = entry
  } else {
    form.itineraries.push(entry)
  }
  itinDlg.visible = false
  genSubsidy(entry)
}

const delItinDlg = reactive({ visible: false, index: -1 })
const prepDelItin = (i) => { delItinDlg.index = i; delItinDlg.visible = true }
const confirmDelItin = () => {
  if (delItinDlg.index >= 0) {
    const rm = form.itineraries[delItinDlg.index]
    form.itineraries.splice(delItinDlg.index, 1)
    removeSubsidyByItinerary(rm)
    recalc()
  }
  delItinDlg.visible = false
}

const genSubsidy = (itin) => {
  const days = daysBetween(itin.departureDate, itin.arrivalDate)
  const rates = subsidyRates(itin.arrivingCityNo)
  removeSubsidyByItinerary(itin)
  const stdPerDay = rates.meal + rates.transport + rates.communication
  form.subsidies.push({
    id: `sub${Date.now()}`,
    mainId: form.main.id || '',
    travelerId: itin.travelerId,
    travelerNo: itin.travelerNo,
    travelerName: itin.travelerName,
    departureDate: itin.departureDate,
    arrivalDate: itin.arrivalDate,
    subsidyDays: String(days),
    departureCity: itin.departureCity,
    departureCityNo: itin.departureCityNo,
    arrivingCity: itin.arrivingCity,
    arrivingCityNo: itin.arrivingCityNo,
    applicationAmount: formatAmount(days * stdPerDay),
    subsidyAmount: '0.00',
    mealAllowance: '0.00',
    transportationAllowance: '0.00',
    phoneAllowance: '0.00',
    businessTypeId: form.main.businessTypeId || '',
    businessTypeNo: form.main.businessTypeNo || '',
    businessTypeName: form.main.businessTypeName || ''
  })
  recalc()
}

const recalc = () => {
  form.main.subsidyTotal = cost.value.subsidyTotal
  form.main.mealAllowance = cost.value.mealTotal
  form.main.transportationAllowance = cost.value.transportTotal
  form.main.phoneAllowance = cost.value.phoneTotal
  recalcAlloc()
}

// —— 补助日历 ——
const subDlg = reactive({ visible: false, index: -1, data: null })

const openSubDlg = (index) => {
  subDlg.index = index
  subDlg.data = form.subsidies[index]
  subDlg.visible = true
}

const saveSubCalendar = (rows) => {
  const sub = subDlg.data
  if (!sub) return
  const key = sub.id || sub.travelerId + sub.departureDate + sub.arrivalDate
  form.calendars = form.calendars.filter(c => c.mainId !== key && c.mainId !== sub.id)
  let mt = 0, tt = 0, pt = 0
  rows.forEach(r => {
    form.calendars.push({
      id: '',
      mainId: key,
      travelDate: r.travelDate,
      travelDateWeek: r.travelDateWeek,
      subsidizedCities: r.subsidizedCities,
      subsidizedCityNumber: r.subsidizedCityNumber,
      standardMealExpensesAmount: r.standardMealExpensesAmount,
      standardTrafficAmount: r.standardTrafficAmount,
      standardCommunicationAmount: r.standardCommunicationAmount,
      mealExpensesAmount: r.mealChecked ? String(r.mealExpensesAmount) : '0',
      trafficAmount: r.transportChecked ? String(r.trafficAmount) : '0',
      communicationAmount: r.phoneChecked ? String(r.communicationAmount) : '0',
      isReimbursed: (r.mealChecked || r.transportChecked || r.phoneChecked) ? '1' : '0'
    })
    if (r.mealChecked) mt += Number(r.mealExpensesAmount) || 0
    if (r.transportChecked) tt += Number(r.trafficAmount) || 0
    if (r.phoneChecked) pt += Number(r.communicationAmount) || 0
  })
  sub.subsidyAmount = formatAmount(mt + tt + pt)
  sub.mealAllowance = formatAmount(mt)
  sub.transportationAllowance = formatAmount(tt)
  sub.phoneAllowance = formatAmount(pt)
  recalc()
  subDlg.visible = false
}

// —— 分摊 ——
const initAlloc = () => {
  if (form.allocations.length === 0) {
    form.allocations.push({
      id: '',
      mainId: '',
      reimCompanyId: '',
      reimCompanyNo: '',
      reimCompanyName: '',
      projectId: '',
      projectNo: '',
      projectName: '',
      allocationRatio: '1.00',
      allocationAmount: cost.value.subsidyTotal,
      _ratioPercent: '100.00'
    })
  }
}

const addAlloc = () => {
  form.allocations.push({
    id: '',
    mainId: '',
    reimCompanyId: '',
    reimCompanyNo: '',
    reimCompanyName: '',
    projectId: '',
    projectNo: '',
    projectName: '',
    allocationRatio: '0.00',
    allocationAmount: '0.00',
    _ratioPercent: '0.00'
  })
  recalcAlloc()
}

const delAllocDlg = reactive({ visible: false, index: -1 })
const prepDelAlloc = (i) => {
  if (form.allocations.length <= 1) {
    ElMessage.warning('至少保留一条分摊信息')
    return
  }
  delAllocDlg.index = i
  delAllocDlg.visible = true
}
const confirmDelAlloc = () => {
  if (delAllocDlg.index >= 0) form.allocations.splice(delAllocDlg.index, 1)
  delAllocDlg.visible = false
  recalcAlloc()
}

const evenAlloc = () => {
  const n = form.allocations.length
  if (!n) return
  const total = Number(cost.value.subsidyTotal) || 0
  const eachAmt = total / n
  const eachRatio = 100 / n
  form.allocations.forEach((a, i) => {
    if (i === 0) return
    a._ratioPercent = eachRatio.toFixed(2)
    a.allocationRatio = (eachRatio / 100).toFixed(4)
    a.allocationAmount = eachAmt.toFixed(2)
  })
  const sumOther = form.allocations.slice(1).reduce((s, a) => s + Number(a._ratioPercent), 0)
  const sumAmtOther = form.allocations.slice(1).reduce((s, a) => s + Number(a.allocationAmount), 0)
  form.allocations[0]._ratioPercent = Math.max(0, 100 - sumOther).toFixed(2)
  form.allocations[0].allocationRatio = (Number(form.allocations[0]._ratioPercent) / 100).toFixed(4)
  form.allocations[0].allocationAmount = (total - sumAmtOther).toFixed(2)
}

const onRatioBlur = (row) => {
  let v = parseFloat(row._ratioPercent)
  if (Number.isNaN(v)) v = 0
  if (v < 0) v = 0
  if (v > 100) v = 0
  row._ratioPercent = v.toFixed(2)
  onRatioChange()
}

const onRatioChange = () => {
  const total = Number(cost.value.subsidyTotal) || 0
  const n = form.allocations.length
  if (n <= 1) {
    if (n === 1) {
      form.allocations[0]._ratioPercent = '100.00'
      form.allocations[0].allocationRatio = '1.00'
      form.allocations[0].allocationAmount = total.toFixed(2)
    }
    return
  }
  let sumOther = 0
  for (let i = 1; i < n; i++) sumOther += Number(form.allocations[i]._ratioPercent) || 0
  if (sumOther > 100) {
    const last = form.allocations[form.allocations.length - 1]
    if (last) last._ratioPercent = ''
    ElMessage.warning('分摊比例合计不能超过100%')
    return
  }
  const r1 = Math.max(0, 100 - sumOther)
  form.allocations[0]._ratioPercent = r1.toFixed(2)
  form.allocations[0].allocationRatio = (r1 / 100).toFixed(4)
  form.allocations[0].allocationAmount = (total * r1 / 100).toFixed(2)
  for (let i = 1; i < n; i++) {
    const ratio = (Number(form.allocations[i]._ratioPercent) || 0) / 100
    form.allocations[i].allocationRatio = ratio.toFixed(4)
    form.allocations[i].allocationAmount = (total * ratio).toFixed(2)
  }
}

const onAmountRowChange = (row) => {
  const total = Number(cost.value.subsidyTotal) || 0
  if (!total) return
  const amt = Number(row.allocationAmount) || 0
  row.allocationRatio = (amt / total).toFixed(4)
  row._ratioPercent = ((amt / total) * 100).toFixed(2)
  onRatioChange()
}

const recalcAlloc = () => {
  const total = Number(cost.value.subsidyTotal) || 0
  const n = form.allocations.length
  if (!n) return
  if (n === 1) {
    form.allocations[0].allocationRatio = '1.00'
    form.allocations[0]._ratioPercent = '100.00'
    form.allocations[0].allocationAmount = total.toFixed(2)
    return
  }
  onRatioChange()
}

// —— 选择器 ——
const onReimbChange = (v) => {
  const e = employees.value.find(x => x.reimburserId === v)
  if (e) {
    form.main.reimburserId = e.reimburserId
    form.main.reimburserNo = e.reimburserNo
    form.main.reimburserName = e.reimburserName
  }
}
const onDeptChange = (v) => {
  const d = departments.value.find(x => x.reimDepartmentId === v)
  if (d) {
    form.main.reimDepartmentId = d.reimDepartmentId
    form.main.reimDepartmentNo = d.reimDepartmentNo
    form.main.reimDepartmentName = d.reimDepartmentName
  }
}
const onCompChange = (v) => {
  const c = companies.value.find(x => x.reimCompanyId === v)
  if (c) {
    form.main.reimCompanyId = c.reimCompanyId
    form.main.reimCompanyNo = c.reimCompanyNo
    form.main.reimCompanyName = c.reimCompanyName
  }
}
const onBizChange = (v) => {
  const bt = findTreeNode(bizTree.value, v)
  if (bt) {
    form.main.businessTypeNo = bt.businessTypeNo
    form.main.businessTypeName = bt.businessTypeName
  }
}
const onAllocCompanyChange = (v, row) => {
  const c = companies.value.find(x => x.reimCompanyId === v)
  if (c) {
    row.reimCompanyId = c.reimCompanyId
    row.reimCompanyNo = c.reimCompanyNo
    row.reimCompanyName = c.reimCompanyName
  } else {
    row.reimCompanyNo = ''
    row.reimCompanyName = ''
  }
}
const onProjChange = (v, row) => {
  const p = projects.value.find(x => x.projectId === v)
  if (p) {
    row.projectNo = p.projectNo
    row.projectName = p.projectName
  }
}

// —— 提交校验 ——
const validate = () => {
  const m = form.main
  if (!m.reimbursementTitle) { ElMessage.warning('请输入报销标题'); return false }
  if (!m.businessTripReason) { ElMessage.warning('请输入出差事由'); return false }
  if (!m.reimburserId) { ElMessage.warning('请选择报销人'); return false }
  if (!m.reimDepartmentId) { ElMessage.warning('请选择报销部门'); return false }
  if (!m.reimCompanyId) { ElMessage.warning('请选择费用归属公司'); return false }
  if (!m.businessTypeId) { ElMessage.warning('请选择业务类型'); return false }
  if (!form.itineraries.length) { ElMessage.warning('请补录至少一条行程'); return false }
  for (const a of form.allocations) {
    if (!a.reimCompanyId) { ElMessage.warning('请选择费用归属'); return false }
  }
  return true
}

const checkDupItin = () => {
  for (let i = 0; i < form.itineraries.length; i++) {
    for (let j = i + 1; j < form.itineraries.length; j++) {
      const a = form.itineraries[i], b = form.itineraries[j]
      if (a.travelerId === b.travelerId && a.departureDate <= b.arrivalDate && b.departureDate <= a.arrivalDate) return true
    }
  }
  return false
}

// 后端消费 main + itineraries + subsidies + calendars + allocations
const buildPayload = () => ({
  main: { ...form.main },
  itineraries: form.itineraries.map(x => ({
    travelerId: x.travelerId,
    travelerNo: x.travelerNo,
    travelerName: x.travelerName,
    departureDate: x.departureDate,
    arrivalDate: x.arrivalDate,
    departureCity: x.departureCity,
    departureCityNo: x.departureCityNo,
    arrivingCity: x.arrivingCity,
    arrivingCityNo: x.arrivingCityNo,
    itineraryInstructions: x.itineraryInstructions
  })),
  subsidies: form.subsidies.map(x => ({ ...x })),
  calendars: form.calendars.map(x => ({ ...x })),
  allocations: form.allocations.map(x => ({
    id: x.id,
    mainId: x.mainId,
    reimCompanyId: x.reimCompanyId,
    reimCompanyNo: x.reimCompanyNo,
    reimCompanyName: x.reimCompanyName,
    projectId: x.projectId,
    projectNo: x.projectNo,
    projectName: x.projectName,
    allocationRatio: x.allocationRatio,
    allocationAmount: x.allocationAmount
  }))
})

const doSave = async () => {
  if (!validate()) return
  if (checkDupItin()) {
    ElMessage.warning('补录行程中存在人员+日期重叠')
    return
  }
  initAlloc()
  try {
    const payload = buildPayload()
    let id
    if (editId.value || form.main.id) {
      payload.main.id = editId.value || form.main.id
      id = await reimApi.update(payload)
    } else {
      id = await reimApi.save(payload)
    }
    form.main.id = id
    saveOkDlg.value = true
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

const closeDlg = ref(false)
const saveOkDlg = ref(false)
const delRemarkDlg = ref(false)

const goList = () => router.push({ path: '/list', query: { _t: String(Date.now()) } })

const confirmClose = () => { closeDlg.value = false; goList() }
const confirmSaveOk = () => { saveOkDlg.value = false; goList() }
const confirmDelRemark = () => { form.main.remarks = ''; delRemarkDlg.value = false }

const loadDetail = async () => {
  if (!editId.value) {
    form.main.creationTime = todayStr()
    initAlloc()
    return
  }
  try {
    const d = await reimApi.detail(editId.value)
    form.main = { ...d.main }
    form.itineraries = d.itineraries || []
    form.subsidies = d.subsidies || []
    form.calendars = d.calendars || []
    form.allocations = (d.allocations || []).map(a => ({
      ...a,
      reimCompanyId: a.reimCompanyId || '',
      reimCompanyNo: a.reimCompanyNo || '',
      reimCompanyName: a.reimCompanyName || '',
      _ratioPercent: toPercent(a.allocationRatio)
    }))
    reimbSel.value = d.main.reimburserId || ''
    deptSel.value = d.main.reimDepartmentId || ''
    compSel.value = d.main.reimCompanyId || ''
    if (!form.allocations.length) initAlloc()
  } catch {
    ElMessage.error('加载失败')
  }
}

onMounted(async () => {
  const [comp, dept, emp, biz, cit, proj] = await Promise.all([
    dictApi.companies(),
    dictApi.departments(),
    dictApi.employees(),
    dictApi.businessTypes(),
    dictApi.cities(),
    dictApi.projects()
  ])
  companies.value = comp
  departments.value = dept
  employees.value = emp
  bizTree.value = buildTree(biz)
  cities.value = cit
  projects.value = proj
  await loadDetail()
})
</script>

<style scoped>
.detail-root {
  min-height: 100vh;
  background: #f0f2f5;
  padding-bottom: 72px;
}
.detail-wrap {
  width: 1200px;
  margin: 0 auto;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  min-height: calc(100vh - 72px);
  display: flex;
  flex-direction: column;
}
.doc-header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 2px solid #409eff;
  background: #fff;
  padding: 0 24px;
}
.doc-title {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin: 0;
}
.doc-date {
  position: absolute;
  right: 24px;
  font-size: 14px;
  color: #606266;
}
.detail-body {
  flex: 1;
  font-size: 14px;
}
.basic-form :deep(.el-form-item__label) {
  font-size: 14px;
}
.hd-link {
  font-size: 14px;
}
.hd-amount {
  color: #e6a23c;
  font-weight: normal;
  margin-left: 8px;
}
.hd-sub {
  font-size: 13px;
  font-weight: normal;
  color: #909399;
  margin-left: 4px;
}
.subsidy-warn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  padding: 8px 12px;
  margin: 0 0 12px;
  font-size: 12px;
  color: #e6a23c;
}
.warn-icon {
  flex-shrink: 0;
  color: #e6a23c;
}
.warn-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: default;
}
.data-table {
  width: 100%;
}
.cost-row {
  display: flex;
  border: 1px solid #ebeef5;
}
.cost-item {
  flex: 1;
  text-align: center;
  padding: 20px 12px;
  border-right: 1px solid #ebeef5;
}
.cost-item:last-child {
  border-right: none;
}
.cost-label {
  display: block;
  color: #606266;
  margin-bottom: 8px;
  font-size: 14px;
}
.cost-val {
  font-size: 16px;
  color: #303133;
}
.req::before {
  content: '*';
  color: #f56c6c;
  margin-right: 2px;
}
.ratio-hd {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}
.even-btn {
  font-size: 12px;
  padding: 0;
}
.ratio-readonly,
.amount-readonly {
  display: block;
  text-align: right;
  padding-right: 8px;
  color: #606266;
}
.ratio-input,
.amount-input :deep(.el-input__inner) {
  text-align: right;
}
.add-row-link {
  text-align: center;
  color: #409eff;
  padding: 12px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.add-row-link:hover {
  opacity: 0.85;
}
.alloc-footer {
  display: flex;
  align-items: center;
  background: #fdf6ec;
  padding: 10px 16px;
  margin-top: 0;
  border: 1px solid #ebeef5;
  border-top: none;
}
.footer-label {
  flex: 1;
  font-weight: bold;
  color: #303133;
}
.footer-ratio {
  width: 150px;
  text-align: right;
  color: #e6a23c;
  font-weight: bold;
  padding-right: 16px;
}
.footer-amt {
  width: 130px;
  text-align: right;
  color: #e6a23c;
  font-weight: bold;
}
.doc-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 200;
  height: 56px;
  background: #fff;
  border-top: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}
.confirm-body {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px;
  font-size: 14px;
}
.confirm-icon {
  color: #67c23a;
}
:deep(.section-panel__bd) {
  padding-top: 12px;
}
</style>
