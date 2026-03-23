## ADDED Requirements

### Player Notes Field
- Player 数据模型增加 `notes` 字段（String，可空，无长度强制限制）
- 新增球员时可填写 notes，不填默认为 null
- 编辑球员时可修改或清空 notes
- 前端球员表单增加备注文本区域（textarea），placeholder 示例："正手强，反手相对弱，发球稳定"，软提示建议 100 字以内
- API `POST /api/teams/{id}/players` 和 `PUT /api/teams/{id}/players/{pid}` 的请求体支持 `notes` 字段
- 已有球员数据不受影响，读取时 notes 默认为 null
- notes 内容在排阵 AI 分析时被引用（若非空）
