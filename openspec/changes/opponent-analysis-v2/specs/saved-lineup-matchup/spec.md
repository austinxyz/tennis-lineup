## MODIFIED Requirements

### AI Prompt 包含球员备注
- `ZhipuAiService.buildPromptWithOpponent()` 在每位球员名后追加 notes（若非空）
- 格式示例：`张三(UTR 6.0, 备注:正手强)` vs 原来的 `张三(UTR 6.0)`
- notes 为空或 null 时不追加，不影响现有格式
- 此变更同时影响最佳三阵的 AI 推荐
