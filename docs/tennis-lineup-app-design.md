# 网球排阵管理系统 - 需求文档

**日期**: 2026-03-11
**版本**: 1.1

---

## 1. 项目概述

### 1.1 项目背景

为一个特定赛制的网球团体比赛准备的管理系统，基于 UTR（Universal Tennis Rating）进行排阵优化。

### 1.2 赛制规则

- **四线双打计分制**: 第一双打(D1: 1分)、第二双打(D2: 2分)、第三双打(D3: 3分)、第四双打(D4: 4分)
- **每线类型**: 可为男双、女双或混双
- **禁止田忌赛马**: 四线的 UTR 需满足 D1 >= D2 >= D3 >= D4

### 1.3 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot |
| 前端 | Vue 3 + Composition API + Tailwind CSS |
| AI 服务 | 智谱 AI Java SDK |
| 存储 | JSON 文件 |

---

## 2. 核心约束（硬约束）

所有排阵必须满足以下硬约束，不满足则排阵失败：

| 约束 | 规则 |
|------|------|
| 最少球员数 | 队伍至少需要 8 名球员才能生成排阵 |
| UTR 排序 | D1 >= D2 >= D3 >= D4（按组合 UTR 降序） |
| 总 UTR 上限 | 上场 8 名队员的个人 UTR 之和不超过 40.5 |
| 女队员要求 | 至少 2 名女队员上场 |
| 搭档差距 | 双打搭档之间的个人 UTR 差距不超过 3.5 |
| D4 要求 | 第四双打两名队员都需要有 100% Verified Doubles UTR |
| 球员唯一性 | 每名球员只能出现在一个组合中 |

**UTR 计算说明**：
- 组合 UTR = 两名球员的个人 UTR 之和
- 总 UTR = 上场 8 名球员的个人 UTR 之和
- 例如：8 名球员 UTR 分别为 4.8, 4.5, 4.3, 4.1, 4.0, 3.9, 3.8, 3.7，总 UTR = 33.1 < 40.5（有效）

---

## 3. 功能需求（按优先级）

### 3.1 组队管理（优先级：高）

- 创建队伍（队伍名称）
- 添加/编辑/删除球员
- 球员信息：姓名、性别、UTR、Verified Doubles UTR 标记
- 批量导入（支持 Excel/CSV 或 JSON）
- 查看/删除队伍

### 3.2 基础排阵生成（优先级：高）

- 选择队伍
- 选择策略：预设策略（均衡/集中火力）或自定义自然语言规则
- 一键生成满足硬约束的排阵

### 3.3 对手策略分析（优先级：中）

- 选择己方队伍
- 选择对手队伍及其排阵
- 输入策略描述（自然语言）
- AI 生成针对性的排阵建议
- 提供胜率分析、优劣势分析

### 3.4 排阵结果可视化（优先级：低）

- 卡片式展示 4 线双打阵容
- 显示每线的 UTR、球员组合
- 策略说明和警告信息

### 3.5 排阵历史记录（优先级：低）

- 保存每次生成的排阵
- 查看历史排阵记录
- 删除不需要的排阵

---

## 4. 数据模型

### 4.1 队伍数据结构

```json
{
  "teams": [
    {
      "id": "team-001",
      "name": "上海飞鹰队",
      "createdAt": "2026-03-11T10:00:00Z",
      "players": [
        {
          "id": "player-001",
          "name": "张三",
          "gender": "male",
          "utr": 5.2,
          "verifiedDoublesUtr": 5.1,
          "verified": true
        },
        {
          "id": "player-002",
          "name": "李四",
          "gender": "male",
          "utr": 4.8,
          "verifiedDoublesUtr": null,
          "verified": false
        }
      ],
      "lineups": [
        {
          "id": "lineup-001",
          "createdAt": "2026-03-11T14:00:00Z",
          "strategy": "均衡策略",
          "pairs": [
            {
              "position": "D1",
              "points": 1,
              "player1Id": "player-001",
              "player1Name": "张三",
              "player2Id": "player-002",
              "player2Name": "李四",
              "combinedUtr": 10.0
            },
            {
              "position": "D2",
              "points": 2,
              "player1Id": "player-003",
              "player1Name": "王五",
              "player2Id": "player-004",
              "player2Name": "赵六",
              "combinedUtr": 9.0
            },
            {
              "position": "D3",
              "points": 3,
              "player1Id": "player-005",
              "player1Name": "孙七",
              "player2Id": "player-006",
              "player2Name": "周八",
              "combinedUtr": 8.0
            },
            {
              "position": "D4",
              "points": 4,
              "player1Id": "player-007",
              "player1Name": "吴九",
              "player2Id": "player-008",
              "player2Name": "郑十",
              "combinedUtr": 7.0
            }
          ],
          "totalUtr": 34.0,
          "valid": true,
          "violationMessages": []
        }
      ]
    }
  ]
}
```

### 4.2 球员数据字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | string | 是 | 唯一标识 |
| name | string | 是 | 姓名 |
| gender | string | 是 | 性别: "male" 或 "female" |
| utr | number | 是 | 个人 UTR |
| verifiedDoublesUtr | number \| null | 否 | Verified Doubles UTR，null 表示无数据 |
| verified | boolean | 是 | 是否有 100% Verified Doubles UTR |

**边界情况处理**：
- `verifiedDoublesUtr` 为 `null` 时，该球员不能用于 D4 位置
- 队伍球员少于 8 人时，返回错误提示"队伍球员不足8人，无法生成排阵"
- 删除球员时，检查该球员是否在已有排阵中使用

### 4.3 设计原则

- 所有队伍统一存储（无"己方/对手"区分，仅在使用时通过 UI 选择）
- JSON 数据结构支持扩展字段
- 排阵关联到具体队伍
- 球员删除前验证引用完整性

---

## 5. API 设计

### 5.1 队伍管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/teams` | 获取所有队伍 |
| GET | `/api/teams/{id}` | 获取单个队伍 |
| POST | `/api/teams` | 创建队伍 |
| PUT | `/api/teams/{id}` | 更新队伍 |
| DELETE | `/api/teams/{id}` | 删除队伍 |
| POST | `/api/teams/{id}/players` | 添加球员 |
| PUT | `/api/teams/{id}/players/{playerId}` | 更新球员 |
| DELETE | `/api/teams/{id}/players/{playerId}` | 删除球员 |
| POST | `/api/teams/import` | 批量导入队伍 |

**更新队伍请求体**：
```json
PUT /api/teams/{id}

{
  "name": "新队名"
}
```
（仅更新队伍名称，球员操作需通过专用接口）

### 5.2 排阵生成 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/lineups/generate` | 生成排阵 |
| POST | `/api/lineups/analyze-opponent` | 针对对手推荐排阵 |
| GET | `/api/teams/{id}/lineups` | 获取队伍排阵历史 |
| DELETE | `/api/lineups/{id}` | 删除排阵 |

### 5.3 排阵生成请求

```json
POST /api/lineups/generate

{
  "teamId": "team-001",
  "strategyType": "preset" | "custom",
  "preset": "balanced" | "aggressive" | null,
  "naturalLanguage": "让前三线尽量强，不考虑D4...",
  "opponentLineupId": "opponent-lineup-001" | null
}
```

**请求说明**：
- `strategyType = "preset"` 时，使用 `preset` 字段，`naturalLanguage` 忽略
- `strategyType = "custom"` 时，使用 `naturalLanguage` 字段，`preset` 忽略
- `opponentLineupId` 为可选，用于对手策略分析
- 请求校验：队伍必须至少有 8 名球员

### 5.4 对手策略分析请求

```json
POST /api/lineups/analyze-opponent

{
  "teamId": "team-001",
  "opponentTeamId": "team-002",
  "opponentLineupId": "opponent-lineup-001",
  "strategyType": "preset" | "custom",
  "naturalLanguage": "采用田忌赛马策略..."
}

// 响应
{
  "success": true,
  "recommendedLineup": { /* 排阵对象 */ },
  "analysis": {
    "winProbability": "high",
    "keyAdvantages": ["D1占优", "D3势均力敌"],
    "riskAreas": ["D2略弱于对手"]
  },
  "alternatives": [
    { /* 备选排阵1 */ },
    { /* 备选排阵2 */ },
    { /* 备选排阵3 */ }
  ]
}
```

**胜率概率值定义**：
- `high`: 预估胜率 > 60%
- `medium`: 预估胜率 40% - 60%
- `low`: 预估胜率 < 40%

**alternatives 数组**：
- 包含最多 3 个备选排阵
- 每个备选排阵为完整的 Lineup 对象

---

## 6. 系统架构

### 6.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                     Vue Frontend                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Team Mgmt   │  │  Lineup Gen  │  │  Opponent    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │ REST API
                           ↓
┌─────────────────────────────────────────────────────────┐
│                  Spring Boot Backend                     │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Controller Layer                    │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌──────────────────────┐  ┌──────────────────────┐   │
│  │  Constraint Service  │  │     AI Service       │   │
│  │  (硬约束验证)         │  │  (策略解析+优化)      │   │
│  └──────────────────────┘  └──────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Data Storage (JSON)                 │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                           │ 智谱 AI SDK
                           ↓
                    ┌──────────────┐
                    │  Zhipu AI    │
                    └──────────────┘
```

### 6.2 前端目录结构

```
src/
├── components/
│   ├── MainLayout.vue       # 主布局（侧边栏 + 主内容区）
│   ├── Sidebar.vue           # 侧边栏导航
│   ├── PlayerForm.vue        # 球员表单
│   ├── LineupCard.vue        # 排阵展示卡片
│   ├── StrategySelector.vue  # 策略选择器
│   └── BatchImport.vue       # 批量导入
├── views/
│   ├── TeamList.vue          # 队伍列表
│   ├── TeamDetail.vue        # 队伍详情 + 球员管理
│   ├── LineupGenerator.vue   # 排阵生成
│   └── OpponentAnalysis.vue  # 对手策略分析
├── router/
│   └── index.ts              # 路由配置
├── api/
│   └── index.ts              # API 封装
└── App.vue
```

### 6.3 后端目录结构

```
src/
├── main/java/com/tennis/
│   ├── TennisApplication.java
│   ├── config/
│   │   ├── ZhipuAiConfig.java      # 智谱 AI 配置
│   │   └── StorageConfig.java      # JSON 存储配置
│   ├── controller/
│   │   ├── TeamController.java
│   │   └── LineupController.java
│   ├── service/
│   │   ├── ConstraintService.java  # 硬约束验证
│   │   ├── AiService.java          # AI 排阵服务
│   │   ├── TeamService.java
│   │   └── LineupService.java
│   ├── model/
│   │   ├── Team.java
│   │   ├── Player.java
│   │   ├── Lineup.java
│   │   └── LineupStrategy.java
│   ├── dto/
│   │   ├── GenerateLineupRequest.java
│   │   └── LineupResponse.java
│   └── repository/
│       └── JsonRepository.java     # JSON 文件存储
└── main/resources/
    └── application.yml
```

---

## 7. 核心服务设计

### 7.1 组合生成算法

**算法描述**：
使用回溯算法生成所有可能的排阵组合。

**算法复杂度**：
- 对于 8 名球员生成 4 对组合，理论复杂度为 O((8)!/(2^4 * 4!)) ≈ O(105)
- 考虑位置顺序（D1-D4），组合数量 ≈ 10,000 级别
- 在 5 秒响应时间要求内可行

**伪代码**：
```java
public List<Lineup> generateAllCombinations(Team team) {
    List<Player> players = team.getPlayers();
    List<Lineup> combinations = new ArrayList<>();

    // 1. 生成所有有效的配对（搭档差距 <= 3.5）
    List<Pair> validPairs = new ArrayList<>();
    for (int i = 0; i < players.size(); i++) {
        for (int j = i + 1; j < players.size(); j++) {
            Pair pair = new Pair(players.get(i), players.get(j));
            if (isValidPair(pair)) {  // 搭档差距检查
                validPairs.add(pair);
            }
        }
    }

    // 2. 回溯生成 4 个互不相交的配对
    backtrack(new Lineup(), validPairs, new HashSet<>(), 0, combinations);

    // 3. 为每个组合分配位置（确保 D1>=D2>=D3>=D4）
    for (Lineup lineup : combinations) {
        assignPositions(lineup);
    }

    return combinations;
}

private void backtrack(Lineup current, List<Pair> pairs, Set<Player> used,
                   int pos, List<Lineup> result) {
    if (pos == 4) {
        result.add(current.copy());
        return;
    }

    for (Pair pair : pairs) {
        if (!used.contains(pair.p1) && !used.contains(pair.p2)) {
            used.add(pair.p1);
            used.add(pair.p2);
            current.pairs[pos] = pair;
            backtrack(current, pairs, used, pos + 1, result);
            used.remove(pair.p1);
            used.remove(pair.p2);
        }
    }
}
```

### 7.2 硬约束服务

```java
@Service
public class ConstraintService {

    // 验证排阵是否满足所有硬约束
    public ValidationResult validateLineup(Lineup lineup, Team team) {
        List<String> violations = new ArrayList<>();

        // 0. 球员唯一性验证
        Set<Player> uniquePlayers = new HashSet<>();
        for (Pair pair : lineup.getPairs()) {
            if (!uniquePlayers.add(pair.getPlayer1())) {
                violations.add("球员 " + pair.getPlayer1().getName() + " 在多个位置出现");
            }
            if (!uniquePlayers.add(pair.getPlayer2())) {
                violations.add("球员 " + pair.getPlayer2().getName() + " 在多个位置出现");
            }
        }

        // 1. UTR 降序验证（按组合 UTR）
        if (!validateUtrOrder(lineup)) {
            violations.add("UTR 不满足 D1 >= D2 >= D3 >= D4");
        }

        // 2. 总 UTR 上限验证 (40.5)
        if (!validateTotalUtr(lineup, team)) {
            violations.add("总 UTR 超过 40.5");
        }

        // 3. 女队员数量验证 (至少2名)
        if (!validateFemalePlayers(lineup)) {
            violations.add("上场女队员少于2名");
        }

        // 4. 搭档 UTR 差距验证 (<= 3.5)
        if (!validatePartnerUtrDiff(lineup)) {
            violations.add("搭档 UTR 差距超过 3.5");
        }

        // 5. D4 Verified Doubles UTR 验证
        if (!validateD4Verified(lineup)) {
            violations.add("D4 队员需要 100% Verified Doubles UTR");
        }

        return new ValidationResult(violations.isEmpty(), violations);
    }
}
```

### 7.3 AI 服务

```java
@Service
public class AiService {

    private final ZhipuAiClient zhipuClient;

    // 生成排阵
    public Lineup generateLineup(
        Team team,
        LineupStrategy strategy,
        Lineup opponentLineup
    ) {
        // 1. 生成所有可能组合
        List<Lineup> allCombinations = generateAllCombinations(team);

        // 2. 过滤满足硬约束的组合
        List<Lineup> validCombinations = allCombinations.stream()
            .filter(l -> constraintService.validateLineup(l, team).isValid())
            .toList();

        if (validCombinations.isEmpty()) {
            throw new NoValidLineupException("无法生成满足约束的排阵");
        }

        // 3. 调用 AI 选择最优解
        return zhipuClient.selectBestLineup(validCombinations, strategy, opponentLineup);
    }
}
```

---

## 8. 智谱 AI SDK 集成

### 8.1 Maven 依赖

```xml
<dependency>
    <groupId>cn.bigmodel.openapi</groupId>
    <artifactId>oapi-java-sdk</artifactId>
    <version>4.0.0</version>
</dependency>
```

### 8.2 配置

```yaml
zhipu:
  api:
    key: ${ZHIPU_API_KEY}
    model: glm-4

storage:
  data-path: ./data
```

### 8.3 AI 服务实现

```java
@Service
public class ZhipuAiService {

    @Value("${zhipu.api.key}")
    private String apiKey;

    public Lineup selectBestLineup(
        List<Lineup> validLineups,
        String strategy,
        Lineup opponentLineup
    ) {
        // 构建提示词
        String prompt = buildPrompt(validLineups, strategy, opponentLineup);

        // 调用智谱 API
        ClientV4 client = new ClientV4.Builder(apiKey).build();
        ChatCompletionRequest request = buildRequest(prompt);

        ChatCompletionResponse response = client.invokeModelApi(request);
        return parseLineupResponse(response);
    }

    private String buildPrompt(List<Lineup> lineups, String strategy, Lineup opponent) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个网球排阵专家。请根据以下信息选择最佳排阵。\n\n");

        sb.append("=== 可选排阵（按组合UTR降序排列）===\n");
        for (int i = 0; i < lineups.size(); i++) {
            Lineup l = lineups.get(i);
            sb.append(String.format("选项%d: ", i + 1));
            sb.append("D1=").append(l.getPairs().get(0).getCombinedUtr());
            sb.append(", D2=").append(l.getPairs().get(1).getCombinedUtr());
            sb.append(", D3=").append(l.getPairs().get(2).getCombinedUtr());
            sb.append(", D4=").append(l.getPairs().get(3).getCombinedUtr());
            sb.append("\n");
        }

        sb.append("\n=== 对手排阵 ===\n");
        if (opponent != null) {
            sb.append("D1=").append(opponent.getPairs().get(0).getCombinedUtr());
            sb.append(", D2=").append(opponent.getPairs().get(1).getCombinedUtr());
            sb.append(", D3=").append(opponent.getPairs().get(2).getCombinedUtr());
            sb.append(", D4=").append(opponent.getPairs().get(3).getCombinedUtr());
        } else {
            sb.append("无");
        }

        sb.append("\n=== 策略 ===\n");
        sb.append(strategy);

        sb.append("\n\n请输出最佳排阵的选项编号（1-").append(lineups.size()).append("）。");
        sb.append("只输出数字，不要解释。");

        return sb.toString();
    }

    private ChatCompletionRequest buildRequest(String prompt) {
        return ChatCompletionRequest.builder()
            .model(GLM_4)
            .messages(List.of(
                ChatMessage.builder().role(Role.SYSTEM).content("你是一个专业的网球排阵顾问。").build(),
                ChatMessage.builder().role(Role.USER).content(prompt).build()
            ))
            .build();
    }

    private Lineup parseLineupResponse(ChatCompletionResponse response) {
        String content = response.getChoices().get(0).getMessage().getContent();
        int selectedIndex = Integer.parseInt(content.trim()) - 1;
        return validLineups.get(selectedIndex);
    }
}
```

### 8.4 AI Fallback 策略

当 AI API 调用失败或超时（> 3 秒）时：
1. 使用确定性启发式算法选择排阵
2. 策略映射：
   - `balanced`: 选择 UTR 方差最小的排阵
   - `aggressive`: 选择前三线 UTR 总和最大的排阵
   - `custom`: 使用 `balanced` 策略

---

## 9. 数据存储与并发控制

### 9.1 JSON 文件存储设计

```java
@Repository
public class JsonRepository {

    private final Path dataPath;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public TeamData readData() {
        lock.readLock().lock();
        try {
            String content = Files.readString(dataPath);
            return objectMapper.readValue(content, TeamData.class);
        } catch (NoSuchFileException e) {
            return new TeamData(); // 返回空数据
        } finally {
            lock.readLock().unlock();
        }
    }

    public void writeData(TeamData data) {
        lock.writeLock().lock();
        try {
            // 先写入临时文件
            Path tempFile = dataPath.resolveSibling(dataPath.getFileName() + ".tmp");
            Files.writeString(tempFile, objectMapper.writeValueAsString(data));

            // 原子性替换
            Files.move(tempFile, dataPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
```

### 9.2 并发控制

- 使用 `ReadWriteLock` 实现读写锁
- 多读单写：允许多个线程同时读取，写入时独占
- 原子性写入：使用临时文件 + 原子替换确保数据一致性

---

## 10. 批量导入

### 10.1 导入格式

**CSV 格式**：
```csv
姓名,性别,UTR,Verified
张三,male,5.2,true
李四,female,4.8,false
王五,male,4.5,true
```

**JSON 格式**：
```json
{
  "teamName": "上海飞鹰队",
  "players": [
    {"name": "张三", "gender": "male", "utr": 5.2, "verified": true},
    {"name": "李四", "gender": "female", "utr": 4.8, "verified": false}
  ]
}
```

### 10.2 错误处理

| 错误情况 | 处理方式 |
|----------|----------|
| 字段缺失 | 跳过该行，记录错误 |
| 数据格式错误 | 跳过该行，记录错误 |
| UTR 非数字 | 跳过该行，记录错误 |
| 性别非 male/female | 跳过该行，记录错误 |
| 总成功数 = 0 | 返回 400，提示无有效数据 |

---

## 11. 安全考虑

### 11.1 输入验证

- 所有用户输入进行类型和范围验证
- UTR 值限制在 0.0 - 16.0 之间
- 队伍名称限制长度（最多 50 字符）
- 防止 SQL 注入（虽然使用 JSON 存储，但仍需注意）

### 11.2 数据安全

- 定期备份 JSON 数据文件
- API Key 通过环境变量配置，不硬编码
- 敏感操作（删除队伍）添加确认机制

---

## 12. 错误处理

### 9.1 错误处理策略

| 错误类型 | 处理方式 |
|----------|----------|
| 约束验证失败 | 返回 400 + 违约列表 |
| 队伍不存在 | 返回 404 |
| AI API 调用失败 | 返回 500 + 友好提示，记录日志 |
| 文件读写错误 | 返回 500 + 错误详情 |

### 9.2 响应格式

```json
// 成功
{
  "success": true,
  "data": { ... }
}

// 失败
{
  "success": false,
  "error": {
    "code": "CONSTRAINT_VIOLATION",
    "message": "排阵不满足硬约束",
    "details": ["UTR 不满足 D1 >= D2 >= D3 >= D4"]
  }
}
```

---

## 13. 前端布局设计

参考 finance 项目，采用侧边栏 + 主内容区布局：

```
┌─────────────────────────────────────────────────────┐
│  MainLayout                                        │
│  ┌──────────┬──────────────────────────────────────┐
│  │          │  顶部栏 (桌面端)                      │
│  │  Sidebar │  ┌──────────────────────────────────┐ │
│  │          │  │ 网球排阵管理                    │ │
│  │  - 队伍  │  └──────────────────────────────────┘ │
│  │    管理  │  ┌──────────────────────────────────┐ │
│  │          │  │          RouterView             │ │
│  │  - 排阵  │  │  (当前页面内容)                 │ │
│  │    生成  │  │                                  │ │
│  │          │  │                                  │ │
│  │  - 对手  │  └──────────────────────────────────┘ │
│  │    分析  │                                      │
│  └──────────┴──────────────────────────────────────┘
└─────────────────────────────────────────────────────┘
```

---

## 14. 非功能需求

- **响应时间**: 排阵生成 < 5 秒（含 AI 调用）
- **并发支持**: 支持 10+ 用户同时使用
- **数据安全**: JSON 文件存储，定期备份
- **可扩展性**: JSON 结构支持字段扩展

---

## 15. 后续迭代方向

- 排阵效果统计与分析
- 多轮比赛记录管理
- 队伍历史战绩追踪
- 移动端适配优化
