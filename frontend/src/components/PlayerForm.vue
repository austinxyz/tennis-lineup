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
          step="0.1"
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
          已验证
        </label>
        <input
          type="checkbox"
          v-model="formData.verified"
          class="rounded border-gray-300 text-blue-600 focus:ring-blue-500" />
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
      verified: false,
    })
  }
})

const formData = ref({ ...props.initialData })
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