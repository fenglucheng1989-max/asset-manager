<template>
  <div>
    <div class="toolbar">
      <h1 class="page-title">协议政策</h1>
      <div class="toolbar-right">
        <el-select v-model="type" style="width: 160px" @change="loadDocuments">
          <el-option label="全部" value="" />
          <el-option label="用户协议" value="TERMS" />
          <el-option label="隐私政策" value="PRIVACY" />
        </el-select>
        <el-button type="primary" @click="openCreate">新增版本</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="documents" v-loading="loading">
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ typeName(row.docType) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="140" />
        <el-table-column prop="version" label="版本" width="130" />
        <el-table-column prop="effectiveDate" label="生效日期" width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="{ row }">{{ row.updatedAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑文档' : '新增版本'" width="760px">
      <el-form label-width="90px">
        <el-form-item label="类型">
          <el-select v-model="form.docType">
            <el-option label="用户协议" value="TERMS" />
            <el-option label="隐私政策" value="PRIVACY" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="form.version" placeholder="例如 2026.04.28" />
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker v-model="form.effectiveDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="14"
            placeholder="建议按“标题换行正文”的格式维护，前端会自动分段展示。"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveDocument">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createLegalDocument, getLegalDocuments, updateLegalDocument } from '../api/admin'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const type = ref('')
const documents = ref([])
const form = reactive({
  id: null,
  docType: 'TERMS',
  title: '',
  version: '',
  effectiveDate: '',
  content: '',
  enabled: true
})

function typeName(value) {
  return value === 'PRIVACY' ? '隐私政策' : '用户协议'
}

function resetForm() {
  Object.assign(form, {
    id: null,
    docType: 'TERMS',
    title: '',
    version: '',
    effectiveDate: '',
    content: '',
    enabled: true
  })
}

async function loadDocuments() {
  loading.value = true
  try {
    documents.value = await getLegalDocuments({ type: type.value })
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    docType: row.docType,
    title: row.title,
    version: row.version,
    effectiveDate: row.effectiveDate,
    content: row.content,
    enabled: row.enabled
  })
  dialogVisible.value = true
}

async function saveDocument() {
  saving.value = true
  try {
    const payload = {
      docType: form.docType,
      title: form.title,
      version: form.version,
      effectiveDate: form.effectiveDate,
      content: form.content,
      enabled: form.enabled
    }
    if (form.id) {
      await updateLegalDocument(form.id, payload)
    } else {
      await createLegalDocument(payload)
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    await loadDocuments()
  } finally {
    saving.value = false
  }
}

onMounted(loadDocuments)
</script>
