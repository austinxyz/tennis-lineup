# 数据导入格式说明

通过 `POST /api/teams/import` 接口批量导入球员，支持 CSV 和 JSON 两种格式。

---

## CSV 格式

文件名须以 `.csv` 结尾。

### 格式规范

- 第一行为表头（固定格式，必须存在）
- 每行一名球员，字段以英文逗号 `,` 分隔
- 编码：UTF-8

### 表头与字段

```
name,gender,utr,verified
```

| 列 | 必填 | 说明 |
|----|------|------|
| `name` | 是 | 球员姓名，不能为空 |
| `gender` | 是 | `male` 或 `female`（不区分大小写）|
| `utr` | 是 | UTR 评分，数字，范围 0.0 ~ 16.0 |
| `verified` | 否 | `true` 或 `false`，默认 `false` |

### 示例文件

```csv
name,gender,utr,verified
张三,male,8.5,true
李四,female,7.2,false
王五,male,10.0,true
赵六,female,6.8,false
```

### 错误处理

- 某行格式有误会跳过该行并记录错误，继续处理后续行
- 表头行始终被跳过
- 字段数量不足 4 个时报错：`字段数量不足，需要: name,gender,utr,verified`

---

## JSON 格式

文件名须以 `.json` 结尾。

### 格式规范

JSON 数组，每个元素为一名球员对象。

```json
[
  {
    "name": "张三",
    "gender": "male",
    "utr": 8.5,
    "verified": true
  },
  {
    "name": "李四",
    "gender": "female",
    "utr": 7.2,
    "verified": false
  }
]
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 球员姓名 |
| `gender` | string | 是 | `"male"` 或 `"female"` |
| `utr` | number | 是 | UTR 评分，范围 0.0 ~ 16.0 |
| `verified` | boolean | 否 | 是否已认证，默认 `false` |

> **注意**：`utr` 字段必须为 JSON 数字类型（`8.5`），不能是字符串（`"8.5"`）。

### 错误处理

- JSON 解析失败时整个文件导入失败，返回解析错误信息
- 单个球员字段验证失败时终止导入并报错

---

## 导入结果

无论 CSV 还是 JSON，导入完成后返回：

```json
{
  "successCount": 3,
  "failureCount": 1,
  "errors": [
    "行 4: UTR必须在0.0到16.0之间"
  ]
}
```

- `successCount`：成功导入的球员数
- `failureCount`：未导入的球员数（含格式错误和验证失败）
- `errors`：错误详情列表，空数组表示全部成功

---

## 常见错误

| 错误信息 | 原因 |
|----------|------|
| `字段数量不足，需要: name,gender,utr,verified` | CSV 行字段少于 4 个 |
| `姓名不能为空` | name 字段为空 |
| `性别必须是male或female` | gender 字段值不合法 |
| `UTR必须是有效的数字` | utr 字段无法解析为数字 |
| `UTR必须在0.0到16.0之间` | utr 超出合法范围 |
| `JSON解析错误: ...` | JSON 文件结构不合法 |
