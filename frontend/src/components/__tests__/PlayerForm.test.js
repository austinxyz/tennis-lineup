import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import PlayerForm from '../PlayerForm.vue'

function mountForm(initialData = {}) {
  return mount(PlayerForm, {
    props: { initialData },
  })
}

describe('PlayerForm', () => {
  // ── 初始渲染 ────────────────────────────────────────────────────────────────

  describe('初始渲染', () => {
    it('默认情况下渲染空表单', () => {
      const wrapper = mountForm()
      expect(wrapper.find('input[type="text"]').element.value).toBe('')
      expect(wrapper.find('select').element.value).toBe('')
      expect(wrapper.find('input[type="number"]').element.value).toBe('')
      expect(wrapper.find('input[type="checkbox"]').element.checked).toBe(false)
    })

    it('传入 initialData 时预填表单', () => {
      const wrapper = mountForm({ name: '张三', gender: 'male', utr: 8.5, verified: true })
      expect(wrapper.find('input[type="text"]').element.value).toBe('张三')
      expect(wrapper.find('select').element.value).toBe('male')
      expect(wrapper.find('input[type="number"]').element.value).toBe('8.5')
      expect(wrapper.find('input[type="checkbox"]').element.checked).toBe(true)
    })

    it('包含姓名、性别、UTR、已验证字段', () => {
      const wrapper = mountForm()
      expect(wrapper.text()).toContain('姓名')
      expect(wrapper.text()).toContain('性别')
      expect(wrapper.text()).toContain('UTR')
      expect(wrapper.text()).toContain('已验证')
    })

    it('性别下拉含 male 和 female 选项', () => {
      const wrapper = mountForm()
      const options = wrapper.find('select').findAll('option')
      const values = options.map(o => o.element.value)
      expect(values).toContain('male')
      expect(values).toContain('female')
    })
  })

  // ── 提交按钮状态 ────────────────────────────────────────────────────────────

  describe('提交按钮状态', () => {
    it('表单为空时提交按钮禁用', () => {
      const wrapper = mountForm()
      const submitBtn = wrapper.findAll('button').find(b => b.text() === '保存')
      expect(submitBtn.element.disabled).toBe(true)
    })

    it('填写完所有必填字段后提交按钮可用', async () => {
      const wrapper = mountForm()
      await wrapper.find('input[type="text"]').setValue('张三')
      await wrapper.find('select').setValue('male')
      await wrapper.find('input[type="number"]').setValue(8.5)
      const submitBtn = wrapper.findAll('button').find(b => b.text() === '保存')
      expect(submitBtn.element.disabled).toBe(false)
    })

    it('只填姓名时提交按钮禁用', async () => {
      const wrapper = mountForm()
      await wrapper.find('input[type="text"]').setValue('张三')
      const submitBtn = wrapper.findAll('button').find(b => b.text() === '保存')
      expect(submitBtn.element.disabled).toBe(true)
    })

    it('UTR 超出范围时提交按钮禁用', async () => {
      const wrapper = mountForm()
      await wrapper.find('input[type="text"]').setValue('张三')
      await wrapper.find('select').setValue('male')
      await wrapper.find('input[type="number"]').setValue(20)
      const submitBtn = wrapper.findAll('button').find(b => b.text() === '保存')
      expect(submitBtn.element.disabled).toBe(true)
    })
  })

  // ── 字段验证 ────────────────────────────────────────────────────────────────

  describe('姓名验证', () => {
    it('姓名为空时 blur 显示错误', async () => {
      const wrapper = mountForm()
      await wrapper.find('input[type="text"]').trigger('blur')
      expect(wrapper.text()).toContain('姓名不能为空')
    })

    it('姓名超过50字符时 blur 显示错误', async () => {
      const wrapper = mountForm()
      await wrapper.find('input[type="text"]').setValue('a'.repeat(51))
      await wrapper.find('input[type="text"]').trigger('blur')
      expect(wrapper.text()).toContain('姓名不能超过50个字符')
    })

    it('姓名合法时无错误', async () => {
      const wrapper = mountForm()
      await wrapper.find('input[type="text"]').setValue('张三')
      await wrapper.find('input[type="text"]').trigger('blur')
      expect(wrapper.text()).not.toContain('姓名不能为空')
      expect(wrapper.text()).not.toContain('姓名不能超过50个字符')
    })
  })

  describe('性别验证', () => {
    it('未选择性别时 change 显示错误', async () => {
      const wrapper = mountForm()
      await wrapper.find('select').trigger('change')
      expect(wrapper.text()).toContain('请选择性别')
    })

    it('选择性别后无错误', async () => {
      const wrapper = mountForm()
      await wrapper.find('select').setValue('female')
      await wrapper.find('select').trigger('change')
      expect(wrapper.text()).not.toContain('请选择性别')
    })
  })

  describe('UTR 验证', () => {
    it('UTR 超过16时 blur 显示错误', async () => {
      const wrapper = mountForm()
      await wrapper.find('input[type="number"]').setValue(17)
      await wrapper.find('input[type="number"]').trigger('blur')
      expect(wrapper.text()).toContain('UTR必须在0.0到16.0之间')
    })

    it('UTR 为负数时 blur 显示错误', async () => {
      const wrapper = mountForm()
      await wrapper.find('input[type="number"]').setValue(-1)
      await wrapper.find('input[type="number"]').trigger('blur')
      expect(wrapper.text()).toContain('UTR必须在0.0到16.0之间')
    })

    it('UTR 合法时无错误', async () => {
      const wrapper = mountForm()
      await wrapper.find('input[type="number"]').setValue(8.5)
      await wrapper.find('input[type="number"]').trigger('blur')
      expect(wrapper.text()).not.toContain('UTR必须在0.0到16.0之间')
    })

    it('UTR 边界值0合法', async () => {
      const wrapper = mountForm()
      await wrapper.find('input[type="number"]').setValue(0)
      await wrapper.find('input[type="number"]').trigger('blur')
      expect(wrapper.text()).not.toContain('UTR必须在0.0到16.0之间')
    })

    it('UTR 边界值16合法', async () => {
      const wrapper = mountForm()
      await wrapper.find('input[type="number"]').setValue(16)
      await wrapper.find('input[type="number"]').trigger('blur')
      expect(wrapper.text()).not.toContain('UTR必须在0.0到16.0之间')
    })
  })

  // ── 事件 ────────────────────────────────────────────────────────────────────

  describe('事件', () => {
    it('点击取消按钮触发 cancel 事件', async () => {
      const wrapper = mountForm()
      await wrapper.findAll('button').find(b => b.text() === '取消').trigger('click')
      expect(wrapper.emitted('cancel')).toBeTruthy()
    })

    it('点击保存按钮触发 submit 事件并携带表单数据', async () => {
      const wrapper = mountForm()
      await wrapper.find('input[type="text"]').setValue('张三')
      await wrapper.find('select').setValue('male')
      await wrapper.find('input[type="number"]').setValue(8.5)
      await wrapper.findAll('button').find(b => b.text() === '保存').trigger('click')
      expect(wrapper.emitted('submit')).toBeTruthy()
      const payload = wrapper.emitted('submit')[0][0]
      expect(payload.name).toBe('张三')
      expect(payload.gender).toBe('male')
      expect(payload.utr).toBe(8.5)
    })

    it('表单无效时点击保存不触发 submit 事件', async () => {
      const wrapper = mountForm()
      // 保存按钮 disabled，click 不会触发 emit
      await wrapper.findAll('button').find(b => b.text() === '保存').trigger('click')
      expect(wrapper.emitted('submit')).toBeFalsy()
    })
  })

  // ── watch initialData ────────────────────────────────────────────────────────

  describe('监听 initialData 变化', () => {
    it('initialData 更新时重置表单数据', async () => {
      const wrapper = mountForm({ name: '张三', gender: 'male', utr: 8.5, verified: false })
      await wrapper.setProps({ initialData: { name: '李四', gender: 'female', utr: 6.0, verified: true } })
      expect(wrapper.find('input[type="text"]').element.value).toBe('李四')
      expect(wrapper.find('select').element.value).toBe('female')
      expect(wrapper.find('input[type="checkbox"]').element.checked).toBe(true)
    })

    it('initialData 更新时清除已有错误', async () => {
      const wrapper = mountForm()
      // 触发错误
      await wrapper.find('input[type="text"]').trigger('blur')
      expect(wrapper.text()).toContain('姓名不能为空')
      // 更新 initialData，错误应清除
      await wrapper.setProps({ initialData: { name: '张三', gender: 'male', utr: 8.5, verified: false } })
      expect(wrapper.text()).not.toContain('姓名不能为空')
    })
  })
})
