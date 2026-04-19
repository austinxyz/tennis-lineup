# Tennis App - Claude Code Guide

> **Context Recovery**: When resuming sessions, read this file first.
> **Project Root**: `C:/Users/lorra/projects/tennis/`

## Stack

Vue 3 + Vite + Tailwind CSS (frontend) / Spring Boot 3.2 + Java 17 + Maven (backend)

## Work Mode

**Default Mode**: Interactive - Ask questions and discuss approaches before implementing.

**Execution Mode**: When user adds `[执行]` prefix to their request, execute directly without asking for confirmation. Make autonomous decisions on implementation details and only report results.

Examples:
- `"添加球员排名功能"` → Interactive mode: Discuss approaches first
- `"[执行] 添加球员排名功能，支持多级分组"` → Execution mode: Implement directly using best practices

## Critical Guardrails

### Environment Setup

**ALWAYS use `npm run dev` to start development server** - Starts the app with hot module replacement.

**Environment Profiles**:
- **dev** (开发环境): Local development via `npm run dev`
  - Hot module replacement enabled
  - Source maps included
  - Development server on port 5173 (Vite default)
- **prod** (生产环境): Build for deployment via `npm run build`
  - Minified code
  - Optimized assets
  - Production server on port 3000

### Frontend Development

**ALWAYS use Composition API** - No Options API. All new components use `<script setup>`.

**ALWAYS use Tailwind CSS utility classes** - Never use CSS variables or custom scoped styles. Project uses Tailwind for all styling.

**NEVER use inline styles** - Use Tailwind utility classes. See `docs/frontend-best-practices.md` for styling patterns.

**ALWAYS format tennis scores properly** - Use `formatTennisScore(score)` helper. Never display raw numbers.

### API Integration

**Family API calls for non-admin users**:
- ✅ Correct: `playerAPI.getPlayer(id)` - Gets specific player
- ❌ Wrong: `playerAPI.getAllPlayers()` - May be restricted, verify access

### Data Handling

**NEVER assume Player objects have all fields** - Handle optional fields gracefully. Use TypeScript interfaces for type safety.

**ALWAYS validate API responses** - Check for required fields before processing.

### Git Workflow

**Use `/git-commit-push` skill** - Stages, commits, and pushes in one step. Follows conventional commits format.

**NEVER force push** - This is a personal project but maintain clean history.

**ALWAYS run tests before commit** - Frontend: `npm test` (when tests exist).

**ALWAYS restart backend after code changes** - The running Spring Boot server uses compiled bytecode. After modifying Java files, kill the current process and restart via `mvn spring-boot:run`. Code changes are NOT hot-reloaded. Every OpenSpec task list MUST include a "restart backend" step after implementation.

## Common Anti-Patterns

❌ **Don't**: Create comprehensive documentation in CLAUDE.md for every feature
✅ **Do**: Document what Claude gets wrong. Point to external docs for details.

❌ **Don't**: Write "Never use feature X" without alternatives
✅ **Do**: Write "Never use X, prefer Y because [reason]"

❌ **Don't**: Execute complex multi-step operations manually
✅ **Do**: Use skills or write simple bash scripts

❌ **Don't**: Update CLAUDE.md with temporary workarounds
✅ **Do**: Fix the underlying issue and document the pattern

❌ **Don't**: Run `npm run build` during development
✅ **Do**: Use `npm run dev` for development with hot reload

## Architecture Quick Reference

```
tennis/
  src/
    components/     # Reusable UI components
    views/          # Page components (lazy-loaded routes)
    router/         # Vue Router config
    api/            # Axios client + API calls
    types/          # TypeScript definitions
    utils/          # Utility functions
    hooks/          # Custom Vue hooks
    stores/         # Pinia stores (if using state management)
```

**Tennis Data Model**: Players, matches, tournaments, rankings. Handle multi-level data relationships.

**Score Calculation**: Use dedicated scoring logic for tennis (15, 30, 40, deuce, advantage). Never hardcode.

## Development Workflow

```bash
# 0. Tool paths (Windows - mvn not in PATH by default)
MVN=/c/Users/lorra/tools/apache-maven-3.9.6/bin/mvn

# 1. Start backend
cd backend && JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 /c/Users/lorra/tools/apache-maven-3.9.6/bin/mvn spring-boot:run

# 2. Frontend development
npm run dev              # HMR at localhost:5173 (Vite default)

# 2. Build for production
npm run build            # Creates dist/ for deployment

# 3. Preview production build
npm run preview          # Serves dist/ at localhost:4173

# 4. Lint and type check
npm run lint             # ESLint check
npm run type-check       # TypeScript check (if using TS)

# 5. Run E2E tests (requires backend + frontend dev server both running)
npm run test:e2e         # Headless Chromium, all tests
npm run test:e2e:ui      # Interactive UI mode

# 6. Commit changes
/git-commit-push

# 7. Update dev log
# Add entry to docs/log/YYYY-MM-DD.md (create if not exists)
```

## Dev Log Practice

**每完成一个功能批次，必须更新当天的开发日志。**

日志文件路径：`docs/log/YYYY-MM-DD.md`（按日期命名，当天若无则新建）

### 每个日志条目包含

```markdown
### N. 功能名称
**提交：** `<git hash>`

**功能：**
- 简洁描述做了什么（bullet points）

**代码审查发现（如有）：**
| 级别 | 问题 | 修复 |

**测试：** X tests 全部通过（新增 Y 个）
```

### 规则

- **每次 commit 后**更新日志（或每个功能批次结束时）
- **待完成事项**用 `- [ ]`，已完成用 `- [x]`
- 日志结尾保留「待完成」章节，列出下一批次或已知问题
- 任务清单中始终包含 **`update log`** 这一步

## When Things Go Wrong

**"Port 5173 is already in use"** → Kill existing process: `taskkill /F /IM node.exe` (Windows)

**"Build failed"** → Check for syntax errors in new components. Run `npm run type-check` for TypeScript issues.

**"Hot reload not working"** → Restart dev server. Sometimes HMR fails on complex state changes.

**"API errors"** → Verify backend is running on correct port. Check CORS configuration.

**"TypeScript errors"** → Install missing types. Update interfaces when API changes.

## External Documentation

For detailed information not covered by these guardrails:

- **Feature requirements**: `requirement/需求说明.md`
- **API contracts**: `requirement/API文档.md`
- **Frontend patterns**: `docs/frontend-best-practices.md`
- **API explorer**: [Add your API docs URL here when available]

## Skills Available

- `/git-commit-push` - Atomic git workflow

## OpenSpec

**ALWAYS run `openspec` commands from the project root** (`C:/Users/lorra/projects/tennis/`), NOT from `frontend/`.

```bash
cd /c/Users/lorra/projects/tennis && openspec list
cd /c/Users/lorra/projects/tennis && openspec new change "my-change"
```

All changes, specs, and archives live under `openspec/` at the project root.

## Context Management

**Session getting slow?** Use this workflow:

1. `/clear` - Clear conversation history
2. `/catchup` - Auto-reads all changed files in current git branch
3. Continue working

**For complex multi-day features**:

1. Ask Claude to document progress in `docs/wip/[feature-name].md`
2. `/clear` to reset
3. Tell Claude to read the WIP doc and continue

**Never use `/compact`** - It's opaque and error-prone. Use `/clear` + `/catchup` instead.