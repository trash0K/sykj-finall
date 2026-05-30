<template>
  <div class="list-page">
    <div class="list-container">
      <div class="search-card">
        <el-form :model="query" inline class="search-form" label-width="100px">
          <el-form-item label="报销单号">
            <el-input v-model="query.id" placeholder="请输入" clearable style="width:200px" />
          </el-form-item>
          <el-form-item label="标题">
            <el-input v-model="query.reimbursementTitle" placeholder="请输入" clearable style="width:200px" />
          </el-form-item>
          <el-form-item label="事由">
            <el-input v-model="query.businessTripReason" placeholder="请输入" clearable style="width:200px" />
          </el-form-item>
          <el-form-item label="费用归属公司">
            <el-select v-model="query.reimCompanyName" placeholder="请选择" clearable style="width:200px">
              <el-option v-for="c in companies" :key="c.reimCompanyId" :label="c.reimCompanyName" :value="c.reimCompanyName" />
            </el-select>
          </el-form-item>
          <el-form-item label="报销部门">
            <el-select v-model="query.reimDepartmentName" placeholder="请选择" clearable style="width:200px">
              <el-option v-for="d in departments" :key="d.reimDepartmentId" :label="d.reimDepartmentName" :value="d.reimDepartmentName" />
            </el-select>
          </el-form-item>
          <el-form-item label="报销人">
            <el-select v-model="query.reimburserName" placeholder="请选择" clearable style="width:200px">
              <el-option v-for="e in employees" :key="e.reimburserId" :label="e.reimburserName" :value="e.reimburserName" />
            </el-select>
          </el-form-item>
          <el-form-item label="业务类型">
            <el-tree-select
              v-model="query.businessTypeName"
              :data="businessTypeTree"
              :props="{ label: 'businessTypeName', value: 'businessTypeName', children: 'children' }"
              placeholder="请选择"
              clearable
              check-strictly
              style="width:200px"
            />
          </el-form-item>
          <el-form-item class="search-actions">
            <el-button plain type="primary" @click="handleAdd">新增</el-button>
            <el-button plain type="primary" @click="handleClear">清除</el-button>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="table-card">
        <el-table
          :data="tableData"
          v-loading="loading"
          border
          style="width:100%"
          :header-cell-style="tableHeaderStyle"
        >
          <el-table-column type="index" label="" width="50" align="center" :index="indexMethod" />
          <el-table-column label="操作" width="140" align="center" fixed="left">
            <template #default="{ row }">
              <div class="op-icons">
                <el-tooltip content="编辑" placement="top">
                  <el-button link type="primary" @click="handleEdit(row)">
                    <el-icon :size="16"><Edit /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button link type="primary" @click="handleDelete(row)">
                    <el-icon :size="16"><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-dropdown trigger="hover" @command="(cmd) => handleCommand(cmd, row)">
                  <el-button link type="primary" class="more-btn">
                    <el-icon :size="16"><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="copy">
                        <el-icon><CopyDocument /></el-icon> 复制
                      </el-dropdown-item>
                      <el-dropdown-item command="submit">
                        <el-icon><Promotion /></el-icon> 提交
                      </el-dropdown-item>
                      <el-dropdown-item command="void">
                        <el-icon><CircleCloseFilled /></el-icon> 作废
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="报销单号" width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="link-text" @click="handleView(row)">{{ row.id }}</span>
            </template>
          </el-table-column>
          <el-table-column label="单据状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.docStatus === '0'" type="info" size="small">草稿</el-tag>
              <el-tag v-else-if="row.docStatus === '1'" type="success" size="small">已完成</el-tag>
              <el-tag v-else-if="row.docStatus === '2'" type="danger" size="small">已作废</el-tag>
              <el-tag v-else type="info" size="small">草稿</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="docType" label="单据类型" width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.docType || '日常报销单' }}</template>
          </el-table-column>
          <el-table-column label="报销人" width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.reimburserName }}[{{ row.reimburserNo }}]</template>
          </el-table-column>
          <el-table-column label="报销部门" width="200" show-overflow-tooltip>
            <template #default="{ row }">[{{ row.reimDepartmentNo }}]{{ row.reimDepartmentName }}</template>
          </el-table-column>
          <el-table-column prop="reimCompanyName" label="费用归属公司" width="180" show-overflow-tooltip />
          <el-table-column prop="businessTypeName" label="业务类型" width="120" show-overflow-tooltip />
          <el-table-column label="报销标题" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="link-text" @click="handleView(row)">{{ row.reimbursementTitle }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="businessTripReason" label="报销事由" min-width="140" show-overflow-tooltip />
          <el-table-column label="补助金额" width="110" align="right" class-name="amount-col">
            <template #default="{ row }">{{ row.subsidyTotal || '0.00' }}</template>
          </el-table-column>
          <el-table-column prop="creationTime" label="创建时间" width="120" />
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @size-change="handleSearch"
            @current-change="handleSearch"
          />
        </div>
      </div>
    </div>

    <ConfirmDialog v-model:visible="delDlg.visible" message="确认删除?" @confirm="confirmDelete" />
    <ConfirmDialog v-model:visible="copyDlg.visible" message="确认复制该报销单?" @confirm="confirmCopy" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Edit, Delete, CopyDocument, MoreFilled, Promotion, CircleCloseFilled } from '@element-plus/icons-vue'
import { reimApi, dictApi } from '../api'
import { buildTree } from '../utils/reim'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

const query = reactive({
  page: 1,
  size: 10,
  id: '',
  reimbursementTitle: '',
  businessTripReason: '',
  reimCompanyName: '',
  reimDepartmentName: '',
  reimburserName: '',
  businessTypeName: ''
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const companies = ref([])
const departments = ref([])
const employees = ref([])
const businessTypeTree = ref([])

const tableHeaderStyle = { background: '#f5f7fa', color: '#303133', fontWeight: 'bold', fontSize: '14px' }
const indexMethod = (i) => (query.page - 1) * query.size + i + 1

const delDlg = reactive({ visible: false, row: null })
const copyDlg = reactive({ visible: false, row: null })

const fetchDict = async () => {
  const [comp, dept, emp, biz] = await Promise.all([
    dictApi.companies(),
    dictApi.departments(),
    dictApi.employees(),
    dictApi.businessTypes()
  ])
  companies.value = comp
  departments.value = dept
  employees.value = emp
  businessTypeTree.value = buildTree(biz)
}

const handleSearch = async () => {
  loading.value = true
  try {
    const res = await reimApi.list({ ...query })
    tableData.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    ElMessage.error(e.message || '查询失败')
  } finally {
    loading.value = false
  }
}

const handleClear = () => {
  Object.assign(query, {
    page: 1,
    size: 10,
    id: '',
    reimbursementTitle: '',
    businessTripReason: '',
    reimCompanyName: '',
    reimDepartmentName: '',
    reimburserName: '',
    businessTypeName: ''
  })
  handleSearch()
}

const handleAdd = () => router.push('/detail')
const handleEdit = (row) => router.push({ path: `/detail/${row.id}`, query: { mode: 'edit' } })
const handleView = (row) => router.push({ path: `/detail/${row.id}`, query: { mode: 'view' } })

const handleDelete = (row) => {
  delDlg.row = row
  delDlg.visible = true
}
const confirmDelete = async () => {
  try {
    await reimApi.delete(delDlg.row.id)
    ElMessage.success('删除成功')
    delDlg.visible = false
    handleSearch()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

const handleCopy = (row) => {
  copyDlg.row = row
  copyDlg.visible = true
}
const confirmCopy = async () => {
  try {
    await reimApi.copy(copyDlg.row.id)
    ElMessage.success('复制成功')
    copyDlg.visible = false
    handleSearch()
  } catch (e) {
    ElMessage.error(e.message || '复制失败')
  }
}

// 更多操作下拉
const handleCommand = (cmd, row) => {
  if (cmd === 'copy') {
    handleCopy(row)
  } else if (cmd === 'submit') {
    submitRow(row)
  } else if (cmd === 'void') {
    voidRow(row)
  }
}

const submitRow = async (row) => {
  try {
    await reimApi.submit(row.id)
    ElMessage.success('提交成功')
    handleSearch()
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  }
}

const voidRow = async (row) => {
  try {
    await reimApi.voidDoc(row.id)
    ElMessage.success('作废成功')
    handleSearch()
  } catch (e) {
    ElMessage.error(e.message || '作废失败')
  }
}

const refreshList = () => {
  query.page = 1
  handleSearch()
}

onMounted(() => {
  fetchDict()
  refreshList()
})

onActivated(() => {
  handleSearch()
})

watch(() => route.query._t, () => {
  if (route.path === '/list') handleSearch()
})
</script>

<style scoped>
.list-page {
  min-height: 100vh;
  background: #f0f2f5;
  padding: 16px;
}
.list-container {
  max-width: 100%;
  margin: 0 auto;
}
.search-card {
  background: #fff;
  padding: 16px 20px 4px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.search-form .el-form-item {
  margin-bottom: 12px;
}
.search-actions {
  margin-left: auto;
}
.table-card {
  background: #fff;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.op-icons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.more-btn {
  padding: 0;
}
.link-text {
  color: #409eff;
  cursor: pointer;
}
.link-text:hover {
  text-decoration: underline;
}
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}
:deep(.amount-col .cell) {
  text-align: right;
}
</style>
