<template>
  <el-dialog
    :model-value="visible"
    :title="editRow?.id ? '编辑补录行程' : '补录行程'"
    width="640px"
    destroy-on-close
    class="itin-dialog"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-alert :closable="false" type="warning" show-icon class="itin-alert">
      <template #title>
        <div class="alert-text">仅可补录未从申请单带入或未产生费用的行程信息</div>
        <div class="alert-text">跨天跨城行程填写说明：出发城市-到达城市：武汉-北京；出发日期-到达日期：1号-5号；1号~5号补助按北京匹配；</div>
      </template>
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="itin-form">
      <el-form-item label="出行人" prop="travelerId" required>
        <el-select v-model="form.travelerId" placeholder="请选择" style="width:100%" @change="onTraveler">
          <el-option v-for="e in employees" :key="e.reimburserId" :label="e.reimburserName" :value="e.reimburserId" />
        </el-select>
      </el-form-item>
      <el-form-item label="出发城市" prop="departureCityNo" required>
        <el-select v-model="form.departureCityNo" placeholder="请选择" clearable filterable style="width:100%" @change="v => setCity(v, 'dep')">
          <el-option v-for="c in cities" :key="c.cityNo" :label="c.cityName" :value="c.cityNo" />
        </el-select>
      </el-form-item>
      <el-form-item label="到达城市" prop="arrivingCityNo" required>
        <el-select v-model="form.arrivingCityNo" placeholder="请选择" clearable filterable style="width:100%" @change="v => setCity(v, 'arr')">
          <el-option v-for="c in cities" :key="c.cityNo" :label="c.cityName" :value="c.cityNo" />
        </el-select>
      </el-form-item>
      <el-form-item label="出发到达日期" prop="dateRange" required>
        <el-date-picker
          v-model="form.dateRange"
          type="datetimerange"
          range-separator=" - "
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width:100%"
        />
      </el-form-item>
      <el-form-item label="行程说明" prop="itineraryInstructions" required>
        <el-input v-model="form.itineraryInstructions" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="行程说明" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { todayStr } from '../utils/reim'

const props = defineProps({
  visible: Boolean,
  editRow: { type: Object, default: null },
  employees: { type: Array, default: () => [] },
  cities: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:visible', 'save'])

const formRef = ref(null)
const form = reactive({
  travelerId: '',
  travelerNo: '',
  travelerName: '',
  departureCityNo: '',
  departureCity: '',
  arrivingCityNo: '',
  arrivingCity: '',
  dateRange: null,
  itineraryInstructions: ''
})

const rules = {
  travelerId: [{ required: true, message: '请选择出行人', trigger: 'change' }],
  departureCityNo: [{ required: true, message: '请选择出发城市', trigger: 'change' }],
  arrivingCityNo: [{ required: true, message: '请选择到达城市', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择出发到达日期', trigger: 'change' }],
  itineraryInstructions: [{ required: true, message: '请输入行程说明', trigger: 'blur' }]
}

const resetForm = () => {
  Object.assign(form, {
    travelerId: '', travelerNo: '', travelerName: '',
    departureCityNo: '', departureCity: '', arrivingCityNo: '', arrivingCity: '',
    dateRange: null, itineraryInstructions: ''
  })
}

watch(() => [props.visible, props.editRow], () => {
  if (!props.visible) return
  if (props.editRow && (props.editRow.travelerId || props.editRow.departureDate)) {
    const r = props.editRow
    form.travelerId = r.travelerId
    form.travelerNo = r.travelerNo
    form.travelerName = r.travelerName
    form.departureCityNo = r.departureCityNo
    form.departureCity = r.departureCity
    form.arrivingCityNo = r.arrivingCityNo
    form.arrivingCity = r.arrivingCity
    form.dateRange = [`${r.departureDate} 00:00:00`, `${r.arrivalDate} 00:00:00`]
    form.itineraryInstructions = r.itineraryInstructions || ''
  } else {
    resetForm()
  }
}, { immediate: true })

const onTraveler = (v) => {
  const e = props.employees.find(x => x.reimburserId === v)
  if (e) {
    form.travelerNo = e.reimburserNo
    form.travelerName = e.reimburserName
  }
}

const setCity = (v, type) => {
  const c = props.cities.find(x => x.cityNo === v)
  if (!c) return
  if (type === 'dep') {
    form.departureCity = c.cityName
    form.departureCityNo = c.cityNo
  } else {
    form.arrivingCity = c.cityName
    form.arrivingCityNo = c.cityNo
  }
}

const handleSave = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const dep = form.dateRange?.[0]?.slice(0, 10)
  const arr = form.dateRange?.[1]?.slice(0, 10)
  if (!dep || !arr) return
  if (arr < dep) {
    ElMessage.warning('到达日期不可早于出发日期')
    return
  }
  if (arr > todayStr()) {
    ElMessage.warning('到达日期不可晚于当前日期')
    return
  }
  const instructions = form.itineraryInstructions.trim()
  if (!instructions) {
    ElMessage.warning('请输入行程说明')
    return
  }
  emit('save', {
    travelerId: form.travelerId,
    travelerNo: form.travelerNo,
    travelerName: form.travelerName,
    departureDate: dep,
    arrivalDate: arr,
    departureCity: form.departureCity,
    departureCityNo: form.departureCityNo,
    arrivingCity: form.arrivingCity,
    arrivingCityNo: form.arrivingCityNo,
    itineraryInstructions: instructions
  })
}
</script>

<style scoped>
.itin-alert {
  margin-bottom: 16px;
}
.alert-text {
  font-size: 12px;
  line-height: 1.6;
}
.itin-form :deep(.el-form-item__label)::before {
  content: '*';
  color: #f56c6c;
  margin-right: 4px;
}
</style>
