<template>
  <div class="bg-white rounded-lg shadow-md p-6">
    <h3 class="text-lg font-semibold mb-4">球员信息</h3>

    <div class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          姓名 *
        </label>
        <input
          type="text"
          v-model="formData.name"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          :class="{ 'border-red-500': errors.name }"
          @blur="validateField('name')" />
        <p v-if="errors.name" class="mt-1 text-sm text-red-600">{{ errors.name }}</p>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          性别 *
        </label>
        <select
          v-model="formData.gender"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          :class="{ 'border-red-500': errors.gender }"
          @change="validateField('gender')">
          <option value="">请选择</option>
          <option value="male">男</option>
          <option value="female">女</option>
        </select>
        <p v-if="errors.gender" class="mt-1 text-sm text-red-600">{{ errors.gender }}</p>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          UTR *
        </label>
        <input
          type="number"
          v-model.number="formData.utr"
          step="0.01"
          min="0"
          max="16"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          :class="{ 'border-red-500': errors.utr }"
          @blur="validateField('utr')" />
        <p v-if="errors.utr" class="mt-1 text-sm text-red-600">{{ errors.utr }}</p>
        <p class="mt-1 text-xs text-gray-500">范围: 0.0 - 16.0</p>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          实际 UTR
        </label>
        <input
          type="number"
          v-model.number="formData.actualUtr"
          step="0.01"
          min="0"
          max="16"
          placeholder="默认同UTR（选填）"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500" />
        <p class="mt-1 text-xs text-gray-500">队长评估的实际水平，留空则默认同官方 UTR</p>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          已验证
        </label>
        <input
          type="checkbox"
          v-model="formData.verified"
          class="rounded border-gray-300 text-blue-600 focus:ring-blue-500" />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          UTR 主页链接
        </label>
        <input
          type="text"
          v-model="formData.profileUrl"
          placeholder="https://app.utrsports.net/profiles/..."
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500" />
        <p class="mt-1 text-xs text-gray-500">可选，用于快速查看球员最新 UTR</p>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          球员特点备注
        </label>
        <textarea
          v-model="formData.notes"
          placeholder="例：正手强，反手相对弱，发球稳定（建议 100 字以内）"
          rows="2"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none" />
        <p class="mt-1 text-xs text-gray-500">可选，AI 分析时会参考此信息</p>
      </div>
    </div>

    <div class="flex justify-end space-x-3 mt-6">
      <button
        @click="$emit('cancel')"
        type="button"
        class="px-4 py-2 bg-gray-300 text-gray-700 rounded-md hover:bg-gray-400">
        取消
      </button>
      <button
        @click="$emit('submit', formData)"
        type="button"
        :disabled="!isValid || Object.keys(errors).length > 0"
        class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50">
        保存
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const emit = defineEmits(['submit', 'cancel'])

const props = defineProps({
  initialData: {
    type: Object,
    default: () => ({
      name: '',
      gender: '',
      utr: null,
      actualUtr: null,
      verified: false,
      profileUrl: '',
      notes: '',
    })
  }
})

const formData = ref({ actualUtr: null, ...props.initialData })
const errors = ref({})

const validateField = (field) => {
  switch (field) {
    case 'name':
      if (!formData.value.name?.trim()) {
        errors.value.name = '姓名不能为空'
      } else if (formData.value.name.length > 50) {
        errors.value.name = '姓名不能超过50个字符'
      } else {
        delete errors.value.name
      }
      break
    case 'gender':
      if (!formData.value.gender) {
        errors.value.gender = '请选择性别'
      } else {
        delete errors.value.gender
      }
      break
    case 'utr':
      if (formData.value.utr === null || formData.value.utr < 0 || formData.value.utr > 16) {
        errors.value.utr = 'UTR必须在0.0到16.0之间'
      } else {
        delete errors.value.utr
      }
      break
  }
}

const validateAll = () => {
  validateField('name')
  validateField('gender')
  validateField('utr')
}

const isValid = computed(() => {
  return formData.value.name?.trim() &&
         formData.value.gender &&
         formData.value.utr !== null &&
         formData.value.utr >= 0 &&
         formData.value.utr <= 16 &&
         Object.keys(errors.value).length === 0
})

watch(() => props.initialData, (newVal) => {
  formData.value = { ...newVal }
  errors.value = {}
}, { deep: true })
</script>