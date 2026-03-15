<template>
  <div class="bg-white rounded-lg shadow-md p-6">
    <h3 class="text-lg font-semibold mb-6">批量导入球员</h3>

    <!-- File Upload -->
    <div class="mb-6">
      <label class="block text-sm font-medium text-gray-700 mb-2">
        选择文件
      </label>
      <div class="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-md hover:border-gray-400 transition-colors">
        <div class="space-y-1 text-center">
          <svg
            class="mx-auto h-12 w-12 text-gray-400"
            stroke="currentColor"
            fill="none"
            viewBox="0 0 48 48">
            <path
              d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round" />
          </svg>
          <div class="flex text-sm text-gray-600">
            <label
              for="file-upload"
              class="relative cursor-pointer bg-white rounded-md font-medium text-blue-600 hover:text-blue-500 focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-blue-500">
              <span>上传文件</span>
              <input
                id="file-upload"
                name="file-upload"
                type="file"
                :accept="acceptedFileTypes"
                @change="handleFileChange"
                class="sr-only" />
            </label>
            <p class="pl-1">或拖拽文件到此处</p>
          </div>
          <p class="text-xs text-gray-500">
            支持 CSV 和 JSON 格式，最大 10MB
          </p>
        </div>
      </div>
      <div v-if="selectedFile" class="mt-2 text-sm text-gray-600">
        已选择: {{ selectedFile.name }}
      </div>
    </div>

    <!-- Format Examples -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
      <div>
        <h4 class="text-sm font-medium text-gray-700 mb-2">CSV 格式示例</h4>
        <pre class="bg-gray-100 p-3 rounded text-xs overflow-x-auto">name,gender,utr,verified
张三,male,1.5,true
李四,female,2.0,false</pre>
      </div>
      <div>
        <h4 class="text-sm font-medium text-gray-700 mb-2">JSON 格式示例</h4>
        <pre class="bg-gray-100 p-3 rounded text-xs overflow-x-auto">[
  {
    "name": "张三",
    "gender": "male",
    "utr": 1.5,
    "verified": true
  },
  {
    "name": "李四",
    "gender": "female",
    "utr": 2.0,
    "verified": false
  }
]</pre>
      </div>
    </div>

    <!-- Import Button -->
    <div class="mb-6">
      <button
        @click="importFile"
        :disabled="!selectedFile || loading"
        class="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:opacity-50 transition-colors">
        <span v-if="loading" class="inline-flex items-center">
          <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          导入中...
        </span>
        <span v-else>开始导入</span>
      </button>
    </div>

    <!-- Import Results -->
    <div v-if="importResult" class="border-t pt-6">
      <h4 class="text-sm font-medium text-gray-700 mb-4">导入结果</h4>

      <div class="bg-green-50 border border-green-200 rounded-md p-4 mb-4">
        <div class="flex items-center">
          <svg class="h-5 w-5 text-green-400" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
          </svg>
          <span class="ml-2 text-green-800">
            成功导入 {{ importResult.successCount }} 条记录
          </span>
        </div>
      </div>

      <div v-if="importResult.failureCount > 0" class="bg-red-50 border border-red-200 rounded-md p-4 mb-4">
        <div class="flex items-center">
          <svg class="h-5 w-5 text-red-400" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
          </svg>
          <span class="ml-2 text-red-800">
            失败 {{ importResult.failureCount }} 条记录
          </span>
        </div>
      </div>

      <div v-if="importResult.errors.length > 0" class="bg-gray-50 border border-gray-200 rounded-md p-4">
        <h5 class="text-sm font-medium text-gray-700 mb-2">错误详情:</h5>
        <ul class="text-sm text-gray-600 space-y-1">
          <li v-for="(error, index) in importResult.errors" :key="index" class="text-red-600">
            - {{ error }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useBatchImport } from '../composables/useBatchImport'

const props = defineEmits(['import-complete'])

const { loading, importResult, importFromCSV, importFromJSON } = useBatchImport()
const selectedFile = ref(null)

const acceptedFileTypes = '.csv,.json'

const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    // Check file size (10MB)
    if (file.size > 10 * 1024 * 1024) {
      alert('文件大小不能超过 10MB')
      return
    }

    selectedFile.value = file
    props.emit('import-complete')
  }
}

const importFile = async () => {
  if (!selectedFile.value) return

  try {
    const fileExtension = selectedFile.value.name.toLowerCase().split('.').pop()
    let result

    if (fileExtension === 'csv') {
      result = await importFromCSV(selectedFile.value)
    } else if (fileExtension === 'json') {
      result = await importFromJSON(selectedFile.value)
    } else {
      alert('不支持的文件格式，请上传 CSV 或 JSON 文件')
      return
    }

    props.emit('import-complete', result)
  } catch (err) {
    alert(`导入失败: ${err.message}`)
  }
}
</script>