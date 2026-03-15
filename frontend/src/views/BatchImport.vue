<template>
  <div class="max-w-2xl mx-auto">
    <h1 class="text-2xl font-bold text-gray-800 mb-6">批量导入</h1>

    <div class="bg-white rounded-lg shadow p-6">
      <p class="text-gray-600 mb-4">上传 CSV 或 JSON 文件以批量导入队伍数据。</p>

      <div
        class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:border-gray-400 transition-colors"
        @dragover.prevent
        @drop.prevent="handleDrop">
        <svg class="w-12 h-12 mx-auto text-gray-400 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
        </svg>
        <p class="text-gray-500 mb-2">拖拽文件至此处，或</p>
        <label class="cursor-pointer">
          <span class="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition-colors text-sm">选择文件</span>
          <input type="file" accept=".csv,.json" class="hidden" @change="handleFileSelect" />
        </label>
        <p class="text-xs text-gray-400 mt-2">支持 .csv 和 .json 格式</p>
      </div>

      <div v-if="selectedFile" class="mt-4 p-3 bg-gray-50 rounded-md flex items-center justify-between">
        <span class="text-sm text-gray-700">{{ selectedFile.name }}</span>
        <button @click="selectedFile = null" class="text-gray-400 hover:text-gray-600">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <button
        v-if="selectedFile"
        @click="handleImport"
        :disabled="loading"
        class="mt-4 w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
        {{ loading ? '导入中...' : '开始导入' }}
      </button>

      <div v-if="error" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-700 text-sm">
        {{ error }}
      </div>

      <div v-if="importResult" class="mt-4 p-4 bg-green-50 border border-green-200 rounded-md">
        <p class="text-green-700 font-medium">导入成功</p>
        <p class="text-green-600 text-sm mt-1">{{ importResult.message || '数据已成功导入' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useBatchImport } from '../composables/useBatchImport'

const { loading, error, importResult, importFromCSV, importFromJSON } = useBatchImport()
const selectedFile = ref(null)

const handleFileSelect = (e) => {
  selectedFile.value = e.target.files[0] || null
}

const handleDrop = (e) => {
  selectedFile.value = e.dataTransfer.files[0] || null
}

const handleImport = async () => {
  if (!selectedFile.value) return
  const ext = selectedFile.value.name.split('.').pop().toLowerCase()
  if (ext === 'json') {
    await importFromJSON(selectedFile.value)
  } else {
    await importFromCSV(selectedFile.value)
  }
}
</script>
