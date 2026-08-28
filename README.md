# caipiao-api

彩票预测与自动投注后端服务，基于 Spring Boot 2.7，配合 Redis 缓存与外部彩票平台 API，实现**定时预测、虚拟投注、中奖结算、真钱下单**等完整流程。

> 仓库地址：https://gitee.com/Collection_jia/caipiao-api

---

## 功能概览

| 模块 | 说明 |
|------|------|
| 定时预测 | 每分钟第 2 秒触发，拉取当期/上期开奖，执行预测与过滤 |
| 虚拟投注 | 按路数模拟扣款，记录投注明细与余额变化 |
| 中奖结算 | 批量比对上期开奖号码，结算奖金并重置计数 |
| 真钱下单 | 可配置 `startpay=true` 后走外部 API 真实下单 |
| 即点真投 | `/api/cp/betNowRealCached` 读取 Redis 缓存出号立刻真投 |
| 多路并行 | 投注、中奖结算、即点真投均支持按路数多线程并行 |
| MySQL 可选 | `mysql.enabled=false` 时可不连库启动，主流程依赖 Redis |

---

## 技术栈

- Java 8+
- Spring Boot 2.7.15
- Redis（Jedis 连接池）
- MyBatis-Plus + Druid（可选）
- Knife4j / SpringDoc OpenAPI
- Hutool HTTP

---

## 项目结构

```
caipiao-api/
├── src/main/java/com/xytl/project/caipiaoapi/
│   ├── CaiPiaoApiApplication.java      # 启动类
│   ├── controller/CaiPiaoApiController.java
│   ├── service/piaocoder/PiaoCoderService.java   # 核心业务
│   ├── config/RedisService.java        # Redis 封装（含批量 mget/mset）
│   └── job/FetchCoderJob.java          # 定时任务
└── src/main/resources/
    ├── application.yml                 # 主配置
    ├── application-test3-local.yml     # 本地/测试环境
    └── application-test3.yml           # 线上 test3 环境
```

---

## 快速开始

### 环境要求

- JDK 8 或以上
- Maven 3.6+
- Redis（必须）
- MySQL（可选，仅历史数据/系统配置需要）

### 本地运行

```bash
# 克隆项目
git clone https://gitee.com/Collection_jia/caipiao-api.git
cd caipiao-api

# 编译
mvn clean package -DskipTests

# 启动（默认 profile: test3-local）
java -jar target/caipiao-api-1.0.0.jar

# 或指定 profile
java -jar target/caipiao-api-1.0.0.jar --spring.profiles.active=test3
```

启动后访问：

- 服务根路径：`http://localhost:8091/cpapi`
- API 文档：`http://localhost:8091/cpapi/doc.html`
- Swagger UI：`http://localhost:8091/cpapi/swagger-ui.html`

---

## 配置说明

### 基础配置（application.yml）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | `8091` | 服务端口 |
| `server.servlet.context-path` | `/cpapi` | 上下文路径 |
| `spring.profiles.active` | `test3-local` | 激活的环境 |
| `mysql.enabled` | `true` | 是否启用 MySQL |
| `caipiao.parallel.enabled` | `true` | 是否启用多路并行 |
| `caipiao.parallel.threads` | `10` | 并行线程池大小 |
| `gametype` | `67` | 游戏类型（48=飞艇，67=赛车） |
| `filepath` | — | 投注/中奖日志文件目录 |

### 环境 Profile

| Profile | 用途 |
|---------|------|
| `test3-local` | 本地开发，默认关闭 MySQL |
| `test3` | 测试/线上环境 |
| `dev` / `prod` | 其他环境配置 |

### Redis 配置

在各 profile 的 `application-*.yml` 中配置：

```yaml
spring:
  redis:
    host: your-redis-host
    port: 6400
    password: your-password
    database: 0
    timeout: 30000
```

### MySQL 可选启动

本地调试若不需要数据库，在 profile 中设置：

```yaml
mysql:
  enabled: false
```

设置为 `false` 后，Spring Boot 会自动排除数据源与 MyBatis 自动配置，服务仍可正常跑预测/投注主流程。

---

## 核心业务流程

每轮定时任务（`loadTimeByCronByjava`）大致分为以下阶段：

```
1. 中奖结算   → countNumWincount（比对上期开奖，结算奖金）
2. 预测出号   → 拉取历史数据，生成 planNumberMap
3. 号码过滤   → 按策略过滤可投号码
4. 虚拟投注   → createOrderConditionbyjavaMuti（模拟扣款）
5. 真实下单   → startpay=true 且 playGame=true 时调用外部 API
6. 查询余额   → 更新 Redis 中的真实余额
```

日志中会输出 `[阶段耗时]`，便于排查性能瓶颈。

---

## 主要 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/cp/ai` | 触发 AI 预测（定时任务也调用此逻辑） |
| GET | `/api/cp/aishow` | 获取页面展示数据（余额、投注、中奖记录） |
| GET | `/api/cp/initdata` | 页面初始化配置 |
| POST | `/api/cp/isopen` | 更新预测/投注开关及策略参数 |
| GET | `/api/cp/startpay` | 开关真实投注 |
| POST | `/api/cp/betNowRealCached` | 即点真投（读缓存出号立刻下单） |
| POST | `/api/cp/updateToken` | 更新采集 Token |
| GET | `/api/cp/getTokenState` | 获取 Token 状态 |
| GET | `/api/cp/getNow` | 获取当期期号与倒计时 |
| POST | `/api/cp/listTopData` | 获取近期开奖数据 |
| POST | `/api/cp/stat` | 近 N 期号码统计分析 |
| GET | `/api/cp/tuncateResult` | 清空预测结果 |

完整接口文档见 Knife4j：`/cpapi/doc.html`

---

## 并行处理

多路投注与中奖结算默认开启并行，按「路数（lushu）」拆分任务：

- **虚拟投注**：各路 `isPayFlagForRoad` 并行，汇总后统一写 `paycountnum`
- **真实下单**：各路 HTTP 下单并行
- **中奖结算**：各路 `agentWinMoney` 并行，批量上下文加锁保证线程安全
- **即点真投**：各路 `betNowRealFromCached` 并行下单

关闭并行（排查问题时）：

```yaml
caipiao:
  parallel:
    enabled: false
```

---

## 部署建议

### 打包

```bash
mvn clean package -DskipTests
```

产物：`target/caipiao-api-1.0.0.jar`

### 生产启动示例

```bash
nohup java -jar caipiao-api-1.0.0.jar \
  --spring.profiles.active=test3 \
  > logs/app.log 2>&1 &
```

### 注意事项

1. **Redis 必须可用**，主流程状态、配置、出号缓存均存 Redis
2. 首次使用前需通过 `/api/cp/updateToken` 设置有效 Token
3. Windows 环境下日志文件写入会自动跳过（`AgentMoney.writefilepath`）
4. 定时任务每分钟执行，同一 ticketId 有 180 秒防重入锁
5. 配置文件中的数据库/Redis 密码请勿提交到公开仓库，建议使用环境变量或外部配置覆盖

---

## 开发说明

### 编译

```bash
mvn compile
```

### 跳过测试打包

```bash
mvn package -DskipTests
```

### 关联前端

本项目通常与 `uniapp-caipiao` 前端配合使用，前端调用 `/cpapi/api/cp/*` 系列接口。

---

## 许可证

私有项目，仅供内部使用。
