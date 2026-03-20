## 1. 后端：ConfigData 模型

- [ ] 1.1 新增 `ConfigData` 模型类：`Map<String, List<ConstraintPreset>> constraintPresets`（key = teamId）
- [ ] 1.2 确认 `ConstraintPreset` 模型已存在（依赖 constraint-presets 功能），若未实现则先创建占位类

## 2. 后端：JsonRepository 扩展双文件支持

- [ ] 2.1 在 `application.yml` 新增 `storage.config-file` 配置项，默认值 `data/tennis-config.json`
- [ ] 2.2 `JsonRepository` 注入 `configFilePath`（`@Value("${storage.config-file:data/tennis-config.json}")`）
- [ ] 2.3 `JsonRepository` 新增独立的 `ReadWriteLock configLock` 和对应的 `Path configPath`
- [ ] 2.4 实现 `readConfig(): ConfigData` 方法，复用现有文件不存在时返回空对象逻辑
- [ ] 2.5 实现 `writeConfig(ConfigData)` 方法，使用 `configLock` 写锁 + 原子写入（temp + rename）
- [ ] 2.6 构造函数中初始化 `configPath`，确保父目录存在

## 3. 后端测试

- [ ] 3.1 更新 `JsonRepositoryTest`：为 config 文件读写提供独立的 `@TempDir` 路径
- [ ] 3.2 新增测试：`readConfig` 文件不存在时返回空 ConfigData
- [ ] 3.3 新增测试：`writeConfig` 原子写入、并发安全
- [ ] 3.4 新增测试：core 文件写锁不阻塞 config 文件读取（两把锁独立）
- [ ] 3.5 运行全量后端测试，确认所有已有测试通过

## 4. 重启后端并验证

- [ ] 4.1 停止当前 Spring Boot 进程，重新执行 `mvn spring-boot:run`
- [ ] 4.2 确认 `tennis-config.json` 在首次启动时自动创建
- [ ] 4.3 确认 `tennis-data.json` 内容完全不变

## 5. 测试报告

- [ ] 5.1 运行 `JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 mvn test` 并记录测试报告（通过数 / 总数 / 失败明细）
