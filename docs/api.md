# API 接口文档

Base URL: `http://localhost:8080/api`

所有请求和响应均使用 `application/json`，编码为 UTF-8。

---

## 错误响应格式

所有错误统一返回以下格式：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "错误描述",
  "details": null
}
```

| code | HTTP 状态 | 说明 |
|------|-----------|------|
| `VALIDATION_ERROR` | 400 | 请求参数无效 |
| `NOT_FOUND` | 404 | 资源不存在 |
| `INTERNAL_ERROR` | 500 | 服务器内部错误 |

---

## 队伍管理

### 获取所有队伍

```
GET /api/teams
```

**响应 200**

```json
[
  {
    "id": "team-1234567890",
    "name": "精英队",
    "createdAt": "2026-03-15T10:00:00Z",
    "players": [...],
    "lineups": [...]
  }
]
```

队伍按创建时间倒序排列（最新的排在前面）。

---

### 获取单个队伍

```
GET /api/teams/{id}
```

**响应 200**：队伍对象（同上）

**响应 404**：队伍不存在

---

### 创建队伍

```
POST /api/teams
Content-Type: application/json
```

**请求体**

```json
{
  "name": "精英队"
}
```

**验证规则**
- `name` 不能为空
- `name` 长度不超过 50 个字符
- `name` 不能与已有队伍重名

**响应 200**：创建成功的队伍对象

**响应 400**：验证失败

---

### 修改队伍名称

```
PUT /api/teams/{id}
Content-Type: application/json
```

**请求体**

```json
{
  "name": "新队名"
}
```

**响应 200**：更新后的队伍对象

**响应 400**：验证失败（名称重复或格式错误）

**响应 404**：队伍不存在

---

### 删除队伍

```
DELETE /api/teams/{id}
```

**响应 204**：删除成功（无响应体）

**响应 404**：队伍不存在

---

## 球员管理

### 获取队伍球员列表

```
GET /api/teams/{id}/players
```

**响应 200**

```json
[
  {
    "id": "player-1234567890",
    "name": "张三",
    "gender": "male",
    "utr": 8.5,
    "verifiedDoublesUtr": 7.0,
    "verified": true
  }
]
```

**响应 404**：队伍不存在

---

### 添加球员

```
POST /api/teams/{id}/players
Content-Type: application/json
```

**请求体**

```json
{
  "name": "张三",
  "gender": "male",
  "utr": 8.5,
  "verifiedDoublesUtr": 7.0,
  "verified": true
}
```

**字段说明**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 球员姓名，1-50 字符，队内唯一 |
| `gender` | string | 是 | `"male"` 或 `"female"` |
| `utr` | number | 是 | UTR 评分，范围 0.0 ~ 16.0 |
| `verifiedDoublesUtr` | number | 否 | 官方认证双打 UTR |
| `verified` | boolean | 否 | 是否已认证，默认 `false` |

**响应 200**：创建成功的球员对象

**响应 400**：验证失败

**响应 404**：队伍不存在

---

### 修改球员信息

```
PUT /api/teams/{id}/players/{playerId}
Content-Type: application/json
```

**请求体**：同添加球员

**响应 200**：更新后的球员对象

**响应 400**：验证失败

**响应 404**：队伍或球员不存在

---

### 删除球员

```
DELETE /api/teams/{id}/players/{playerId}
```

**响应 204**：删除成功（无响应体）

**响应 404**：队伍或球员不存在

---

## 批量导入

### 从文件导入球员

```
POST /api/teams/import
Content-Type: multipart/form-data
```

**参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `file` | file | CSV 或 JSON 文件，文件名后缀决定解析方式 |

支持的文件格式：`.csv`、`.json`（详见[导入格式说明](import-format.md)）

**响应 200**

```json
{
  "successCount": 3,
  "failureCount": 1,
  "errors": [
    "行 4: UTR必须在0.0到16.0之间"
  ]
}
```

**响应 400**：文件为空或格式不支持
