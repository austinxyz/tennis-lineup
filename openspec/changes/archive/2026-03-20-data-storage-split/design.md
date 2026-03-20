## Context

当前 `JsonRepository` 管理单个 `tennis-data.json`，使用一个 `ReadWriteLock` 保证并发安全，原子写入（temp file + rename）保证数据完整性。`TeamData` 是根对象，包含 `teams: List<Team>`。

新设计引入第二个文件 `tennis-config.json`，用于存放配置类数据（从 constraint-presets 功能开始）。两个文件各自独立管理，互不干扰。

## Goals / Non-Goals

**Goals:**
- 拆分存储为两个文件：`tennis-data.json`（核心）和 `tennis-config.json`（配置）
- 两个文件各有独立的 ReadWriteLock 和原子写入
- Service 层以上接口完全不变（零感知重构）
- `application.yml` 支持两个文件路径独立配置

**Non-Goals:**
- 不做按队伍分文件（方案 A 已排除）
- 不迁移现有 `tennis-data.json` 中的任何已有字段（Team/Player/Lineup 留原位）
- 不改变 API 接口或前端任何内容

## Decisions

### 1. JsonRepository 扩展：双文件双锁
`JsonRepository` 新增第二套 `configFilePath` + `ReadWriteLock configLock`，对应 `readConfig()` / `writeConfig(ConfigData)` 方法，原有 `read()` / `write(TeamData)` 完全不变。

**理由**：最小改动，零破坏性。两把锁独立，核心数据写锁不阻塞配置读取。

### 2. ConfigData 模型
```java
public class ConfigData {
    private Map<String, List<ConstraintPreset>> constraintPresets = new HashMap<>();
    // key = teamId，value = 该队伍的预设列表
}
```
选择 Map 而非在 Team 内嵌套，原因：config 文件独立于核心数据文件，不需要 Team 对象即可读写预设，避免跨文件 join。

**备选**：在 `TeamData.Team` 内加 `constraintPresets` 字段。→ 需要把配置数据混回核心文件，失去分离意义，排除。

### 3. 配置文件路径配置
```yaml
storage:
  data-file: data/tennis-data.json      # 已有
  config-file: data/tennis-config.json  # 新增
```
路径独立可配，测试时各用 `@TempDir` 子路径隔离。

### 4. 首次启动自动创建
`JsonRepository` 构造函数已有"文件不存在则创建"逻辑，config 文件复用同一逻辑，写入空 `ConfigData {}`。

### 5. 无数据迁移脚本
`constraintPresets` 是 constraint-presets 功能新增字段，尚未在任何环境存入真实数据，无需迁移。若后续需要，在启动时一次性检测并迁移。

## Risks / Trade-offs

- **双文件不一致**：两次写入之间宕机可能导致数据状态不一致（如预设已保存但队伍已删）。→ 接受，当前业务场景下不一致影响极低；未来若需强一致可引入简单事务日志。
- **测试改动**：`JsonRepositoryTest` 和相关测试需提供两个路径。→ 改动量小，风险低。
- **配置文件路径未配置**：若 `storage.config-file` 缺失，Spring 启动报错。→ 在 `application.yml` 设置默认值 `data/tennis-config.json`，`@Value` 注解加 `defaultValue`。

## Migration Plan

1. 新增 `ConfigData` 模型和 `JsonRepository` config 方法（不影响现有运行）
2. 新增 `application.yml` 配置项（有默认值，不影响现有部署）
3. `ConstraintPresetService` 使用新的 `readConfig()`/`writeConfig()` 接口
4. 部署：重启后端，`tennis-config.json` 自动创建，现有 `tennis-data.json` 完全不变
5. 回滚：删除 `tennis-config.json`，恢复旧代码，无数据损失（config 数据仅为预设，可重建）
