# 作业业务操作记录

## 入口与职责

作业详情顶部“参数”后增加“操作记录”。宽侧滑面板接收 `jobId`，自行管理关键字、CombineSearcher、分页、记录选择和详情请求。后台保持现有运行记录不变。

所有操作 API 通过 Handler 工厂进入 `AutoexecJobActionHandlerBase.doService`，包括撤销和 SQL 状态重置。公共处理链根据处理器的 `JobAction` 白名单调用 `IJobOperationAuditService`，由自动化模块实现快照和独立存储，API 无审计包装。SQL 快照通过来源接口适配，基础模块不依赖具体实现。

最外层白名单动作建立上下文并在 finally 中清理，嵌套动作不重复采集，连续独立调用各记录一次。自动调度、创建流程经过处理链的白名单动作同样记录，只读动作不记录。进入处理链前的校验拒绝不创建记录，处理链内的验证异常保留请求快照，不保存结果或前后状态。

## 按钮与接口覆盖

以下路径均位于 `/api/rest/autoexec/job/` 下；每次最外层动作调用创建一条主记录。

| 页面操作 | 接口后缀 | JobAction 持久化编码 |
| --- | --- | --- |
| 执行作业 | `from/combop/execute` | `fire` |
| 重新执行作业 | `refire` | `refireAll`，记录重跑策略 |
| 暂停、终止、验证、接管 | `pause`、`abort`、`check`、`takeover` | 对应业务动作 |
| 撤销 | `revoke` | `revoke` |
| 阶段执行/全部执行 | `phase/refire` | `refirePhase`，记录重跑策略 |
| 本地 Runner 阶段忽略 | `phase/ignore` | `ignorePhase` |
| 阶段重置，节点/SQL 单个、选中、全部重置 | `phase/node/reset` | `resetNode`，按执行模式解析目标类型 |
| 节点/SQL 单个、选中、全部忽略 | `phase/node/ignore` | `ignoreNode` |
| 节点/SQL 重新执行 | `phase/node/refire` | `refireNode` |
| 人工动态按钮、输入与选择后继续 | `phase/node/submit/waitInput` | `submitNodeWaitInput` |
| SQL 状态重置接口 | `sql/status/reset` | `resetSql` |

查看、日志复制、下载、导出、切换及取消确认不创建本功能记录。SQL 按文件而不是单条语句记录。

显式 `isAll=1` 的重置和忽略操作统一归为阶段对象，操作对象显示阶段名称，仅保存阶段目标快照。单个或选中批量仍按节点／SQL 文件记录，即使选中了当前全部对象，也不会自动归为阶段。业务动作编码与执行逻辑保持不变。

## 动作统一

`JobAction` 是审计动作的唯一维护入口，集中定义对象类型及展示分组。记录保存业务动作编码，查询按分组匹配对应的 JobAction 编码；未上线的临时审计编码不提供兼容映射。只读动作和运行通知不参与审计选项。枚举元数据不会自动扩大采集入口。

搜索响应始终包含 `actionList`（`value`、`text`），空结果也返回完整的去重选项。前端直接使用选项，动作展示名由后端 i18n 生成，现有业务 `getText()` 保持不变。

## 查询与记录语义

- `operation/audit/search`：`jobId` 必填；可传 `keyword`、`objectType`、`action`、`operatorUuid`、`startTime`、`endTime`、`currentPage`、`pageSize`。
- 时间条件使用毫秒时间戳；关键字和组合条件取交集；操作人兼容 `user#UUID`。
- `operation/audit/get`：`jobId` 和记录 `id` 必填，分页参数用于目标明细。
- 默认每页 20 条，最大 100 条；失效页码收敛至末页；列表按时间和 ID 倒序。大整数 ID 以字符串输出。
- 查询使用租户数据源隔离，沿用当前作业详情的 `AUTOEXEC_BASE` 权限；详情同时校验作业和记录归属。
- 记录请求及操作前的目标快照，不追踪成功、失败、受理、跳过或业务状态；记录存在不代表业务执行成功。
- 记录通过独立事务保存，不随业务回滚；接管及交互上下文在入口执行后补充，不注册业务事务结果回调。
- 执行用户按 UUID 查询当前名称，同页内复用查询；用户不存在时显示 UUID，不再保存用户名和作业名的独立快照。
- 自由输入始终脱敏；按钮和选择值仅在匹配服务端定义时保留。记录不保存完整请求、SQL 正文或异常原文。
- 审计故障记录错误日志，不改变原业务返回，也不重试原操作。本期没有无损恢复队列。

## 发布

先应用 `changelog/2026-09-16/neatlogic_tenant.sql`，再发布后端和前端。新增两张租户表及相应表定义，没有对业务表增加外键。

历史记录不回填；不新增审计删除、导出或保留期配置。SQL 状态重置的直接接口按所属作业解析阶段名称后再调用来源处理器。

删除作业时，在作业删除事务内按 `jobId` 先清理操作目标明细，再清理操作主记录；任一步失败均向上传播并整体回滚，不使用审计写入的独立事务。

## 验证

```sh
mvn -f ../neatlogic-build-root/pom.xml -Pdevelop,commercial -q '-Dtest=AutoexecJobOperationAudit*Test' -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false -pl ../neatlogic-autoexec,../neatlogic-deploy -am test
node ../neatlogic-web/scripts/test-job-operation-audit.cjs
```

Java 测试使用内存存储、Mapper 和 SQL 来源替身；前端测试编译真实 Vue 模板并控制模拟接口的响应顺序，不连接真实 Runner、不执行作业或 SQL。

数据库结构尚未上线，调整新增建表脚本及表定义，不提供历史字段兼容。本次验证不修改现有数据库。
