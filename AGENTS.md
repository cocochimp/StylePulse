# AGENTS.md — Reflow 项目开发规范

> 本文件是**项目级硬约束**，对本仓库所有代码生成、修改、评审生效。
> 全局约定（简体中文回复、先问再做、提交格式 `feat: xxx`）见 `~/.dsh/AGENTS.md`，本文件只补项目专属规则。
> DDD 规范基线：[easy-DDD](https://github.com/alizhangsan602-bit/easy-DDD)（六边形架构 + 六层分层 + 四种开发模式）。

---

## 1. 项目定位与边界

**Reflow = SHEIN 供货商侧的返单决策中台。** 只做返单决策链：诊断 → 规则命中 → 追单量 → 拆量 → 产能预占 → 复盘。

### 1.1 明确不做（禁止擅自扩展）

| 禁止扩展的方向 | 原因 |
|---|---|
| 选品、买手、选款 | 商业判断，不是系统职责 |
| 采购下单、面辅料管理 | 属于 ERP，本系统只输出追单建议 |
| 定价、核价、利润核算 | 不碰价格 |
| 履约跟踪、物流轨迹 | 只关心"到仓"节点 |
| 财务结算、发票、账务 | 不做账 |
| 多平台通用化、抽象出"通用补货引擎" | 通用化会毁掉规则可归因性 |
| 微服务拆分 | 千级款规模，单体模块化足够；边界留好即可，不提前拆 |

需要新增上下文或跨域依赖时，**先问，不要自行动手**。

### 1.2 限界上下文（五个，不可随意增减）

| 上下文 | 聚合根 | 职责 |
|---|---|---|
| `style` | `StyleAggregate` | 款主数据、颜色尺码矩阵、生命周期 |
| `metric` | `SalesWatermarkAggregate` | 销量事件、分桶滑窗、健康度 |
| `replenish` | `ReplenishRuleAggregate` / `ReplenishDecisionAggregate` | 规则、追单量、决策单、拆量 |
| `capacity` | `CapacitySlotAggregate` | 产能槽、预占、生产指令 |
| `insight` | 无 | 兑现率、命中率、影子对比（纯计算） |

---

## 2. 技术栈

| 项 | 版本/选型 |
|---|---|
| JDK | 17 |
| 框架 | Spring Boot 3.x |
| 持久层 | MyBatis-Plus + MySQL 8 |
| 分片 | ShardingSphere-JDBC（`style_id` / `factory_id`） |
| 缓存 | Redis 7（Lua、Bitmap） |
| 本地缓存 | Caffeine |
| 位图 | `org.roaringbitmap:RoaringBitmap` |
| 消息 | RocketMQ（普通消息 + 延迟消息） |
| 监控 | Micrometer + Prometheus + Grafana |
| 压测 | JMeter |

**新增依赖前必须确认层级**：位图库、缓存库、中间件客户端一律只允许出现在 `infrastructure` 与 `adaptor`。

### 2.1 模块清单（Maven 多模块，依赖由 pom 强制）

| 模块 | 职责 | 允许依赖 | 禁止依赖 |
|---|---|---|---|
| `reflow-common` | 技术基座：`ResultDO`、`Base*`、异常、`LevelLock`、`Field`。**不含任何业务概念** | 无 | 全部 |
| `reflow-model` | 跨上下文共享的业务枚举与常量 | common | 其他 |
| `reflow-client` | DTO + AppService 接口 | common | model / domain / application |
| `reflow-domain` | 聚合根、实体、值对象、Param、Result、DomainService、Repository 接口 | common、model | 其他 |
| `reflow-application` | 场景编排、Assembler、事务边界 | common、model、client、domain | infrastructure |
| `reflow-infrastructure` | 仓储实现（MyBatis-Plus / Redis / Lua / 位图） | common、model、domain | application、client |
| `reflow-adaptor` | input（Controller/消费者/定时任务）+ output（工厂接口） | common、model、client、application | infrastructure |
| `reflow-bootstrap` | 唯一启动与装配模块 | 全部 | — |

> `reflow-common` 是六层之外的**共享内核**：它只装"所有层都要用、且与业务无关"的东西。
> 不要往里加业务枚举（那是 model 的事），也不要往里加工具类堆场。

---

## 3. 分层与依赖规则

### 3.1 调用方向

```
adaptor/input  →  application  →  domain  →  repository(接口)
                                            ↘  adaptor/output（由 application 调用）
infrastructure ── 实现 domain 的 repository 接口
```

### 3.2 依赖矩阵（✓ 允许，✗ 禁止）

| 依赖方 \ 被依赖 | common | domain | application | client | model | infrastructure | adaptor |
|---|---|---|---|---|---|---|---|
| **domain** | ✓ | ✓ | ✗ | ✗ | ✓ | ✗ | ✗ |
| **application** | ✓ | ✓ | ✗ | ✓ | ✓ | ✗ | ✗ |
| **client** | ✓ | ✗ | ✗ | ✗ | **✗** | ✗ | ✗ |
| **infrastructure** | ✓ | ✓ | ✗ | ✗ | ✓ | ✗ | ✗ |
| **adaptor** | ✓ | ✗ | ✓(调用) | ✗ | ✓ | ✗ | ✗ |

三条最容易踩的线：

1. **`client` 禁止依赖 `model`**——client 会被外部系统依赖，DTO 必须自包含，否则依赖扩散。
2. **`domain` 禁止依赖 `adaptor`**——领域层要外部数据时，用 Param 里的函数式接口（见 §9.3）。
3. **`adaptor/input` 禁止反向依赖 `domain`**——入口只调 `application` 定义的 `AppService` 接口；`adaptor/output` 实现 `application` 定义的 Adaptor 接口，领域层对第三方调用完全无感知。

### 3.3 中间件归属

| 能力 | 归属 | 说明 |
|---|---|---|
| MySQL、Redis、MQ、分布式锁 | `infrastructure` | 通过 `domain/xxx/repository` 接口暴露 |
| 第三方 HTTP（工厂 MES、ERP、告警） | `adaptor/output` | 由 `application` 调用，domain 不可见 |
| Controller、MQ 消费者、定时任务 | `adaptor/input` | 只做协议转换与参数校验，禁止写业务逻辑 |

---

## 4. 包结构模板

```
com.reflow
├── common/                     # 技术基座：result / base / exception / lock / field
├── domain/
│   ├── style/
│   │   ├── model/
│   │   │   ├── aggregate/     # StyleAggregate
│   │   │   ├── entity/        # SkuEntity
│   │   │   ├── value/         # SizeCurveValue
│   │   │   ├── param/         # ChangeStageParam
│   │   │   └── result/        # ...
│   │   ├── service/           # StyleDomainService / Impl
│   │   └── repository/        # StyleRepository（接口）
│   ├── metric/
│   ├── replenish/
│   ├── capacity/
│   └── insight/
├── application/
│   ├── style/{scenario,assembler,model}
│   ├── replenish/{scenario,assembler,model}
│   └── ...
├── client/
│   ├── style/{enums,model,req,res}
│   └── replenish/{enums,model,req,res}
├── model/                      # 跨域共享：StageEnum、BusinessDateValue 等
├── infrastructure/
│   ├── style/{repository,mysql/{po,mapper},converter}
│   ├── replenish/{repository,mysql/{po,mapper},converter}
│   └── ...
└── adaptor/
    ├── style/input/            # StyleController
    ├── metric/input/           # SalesEventMessageConsumer
    ├── capacity/{input,output,inner}
    └── ...
```

### 4.1 `domain` 层只允许出现这七类代码

聚合根、实体、值对象、Param、Result、DomainService、Repository 接口。

**禁止出现**：`Utils`、常量类、配置类、枚举工具、`@Component` 之外的 Bean 定义、`@Value` 配置注入、`RedisTemplate`、`ObjectMapper`、`DateTimeFormatter` 静态实例等任何非领域概念。

---

## 5. 四种开发模式

### 5.1 模式选择决策树

```
需要修改数据状态？
├── 是 → 写模式
└── 否 → 有业务逻辑？
        ├── 否 → 读模式
        └── 是 → 逻辑基于可配置规则？
                ├── 是 → 规则+计算模式
                └── 否 → 纯计算模式
```

### 5.2 本项目场景映射（照这张表落位，不要自创）

| 业务场景 | 模式 | 聚合根 | DomainService | AppService |
|---|---|---|---|---|
| 款状态流转、开决策单、审批、下发 | **写** | `StyleAggregate`、`ReplenishDecisionAggregate` | `ReplenishDomainService` | `ReplenishDecisionAppService` |
| 销量事件接入、水位更新 | **写** | `SalesWatermarkAggregate` | `SalesWatermarkDomainService` | `SalesEventAppService` |
| 产能预占/确认/释放 | **写** | `CapacitySlotAggregate` | `CapacityDomainService` | `CapacityAppService` |
| 规则匹配 + 追单量计算 | **规则+计算** | `ReplenishRuleAggregate` | `ReplenishCalculateDomainService` | `ReplenishCalculateQueryAppService` |
| 尺码矩阵拆量 | **纯计算** | 无 | `SizeAllocationDomainService` | `SizeAllocationQueryAppService` |
| 健康度评分、DOS、动销率 | **纯计算** | 无 | `StockHealthDomainService` | `StockHealthQueryAppService` |
| 影子对比、兑现率复盘 | **纯计算** | 无 | `ShadowCompareDomainService` | `ShadowCompareQueryAppService` |
| 决策单列表、款详情、看板 | **读** | 聚合根仅作数据载体 | 无 | `XxxQueryAppService` |

**判断口诀**：会改状态 → 写；只查不改 → 读；有规则表 → 规则+计算；纯公式 → 纯计算。

---

## 6. 各层红线

### 6.1 `domain`

- ❌ 禁止使用任何设计模式（策略、工厂、模板方法、责任链、状态模式等）。业务分支用 `if/else` 或 `switch` 直接写在聚合根/实体/DomainService 里。
  - 理由：领域层要能一眼读完业务规则；分支数量有限、可穷举，不需要为扩展性抽象。
- ❌ 禁止设计与技术相关的通用方法（如 `update`、`save`、`process`），方法名必须是业务动词（`approve`、`markDispatched`、`calculateQty`）。
- ❌ 禁止在 DomainService 中持有成员变量（除注入的 Repository）。
- ❌ 禁止调用其他上下文的服务或聚合根。
- ✅ 允许依赖 Spring 注解（`@Service`）、静态工具包、`model` 层。

### 6.2 `application`

- ❌ 禁止直连数据库、Redis、MQ——一律通过 Repository / Adaptor。
- ❌ 禁止包含核心业务规则（状态校验、计算公式、规则匹配）。
- ✅ 允许编排：参数校验 → 调 Adaptor 取外部数据 → 组装 Param → 调 DomainService → 转 DTO。
- ❌ Command 层禁止调用 Query 层（CQRS 约束）。

### 6.3 `infrastructure`

- ❌ 禁止调用第三方 HTTP 接口（属于 `adaptor/output`）。
- ❌ 禁止在 Converter 里写业务判断。
- ✅ 允许：Redis Lua、RoaringBitmap 位图、Caffeine、分片、分布式锁、发消息、写日志。

### 6.4 `adaptor`

- ❌ `adaptor/input` 禁止写业务逻辑，只做协议转换 + 参数自校验。
- ✅ `adaptor/output` **允许使用策略模式**做技术适配（如按 `factoryId` 路由不同工厂接口、按渠道选择不同客户端）——这是技术适配，不是业务逻辑。

### 6.5 `client`

- ❌ 禁止依赖 `model`、`domain`、`application`。
- ✅ 只放 RequestDTO / ResponseDTO / 枚举 / 入参模型。

---

## 7. 命名规范

| 类型 | 命名规则 | 示例 | 所在层 |
|---|---|---|---|
| 聚合根 | `{名词}Aggregate`，继承 `BaseAggregate` | `ReplenishDecisionAggregate` | domain |
| 实体 | `{名词}Entity`，继承 `BaseEntity` | `SkuEntity` | domain |
| 值对象 | `{名词}Value`，继承 `BaseValue` | `SizeCurveValue` | domain |
| 入参对象 | `{方法名}Param`，继承 `BaseParam` | `CalculateQtyParam` | domain |
| 返回对象 | `{方法名}Result`，继承 `BaseResult` | `CalculateQtyResult` | domain |
| 领域服务（写） | `{聚合根名}DomainService` / `Impl` | `ReplenishDomainService` | domain |
| 领域服务（计算） | `{动词}DomainService` / `Impl` | `SizeAllocationDomainService` | domain |
| 仓储接口 | `{业务名}Repository`，继承 `AggregateRepository` | `StyleRepository` | domain |
| 仓储实现 | `{业务名}RepositoryImpl`，`@Component` | `StyleRepositoryImpl` | infrastructure |
| PO | `{表对应业务名}PO` | `ReplenishDecisionPO` | infrastructure |
| Mapper | `{表对应业务名}Mapper` | `ReplenishDecisionMapper` | infrastructure |
| 转换器 | `{业务名}Converter`，静态方法 `toAggregate`/`toPO` | `StyleConverter` | infrastructure |
| 应用服务（写） | `{聚合根名}AppService` / `Impl` | `ReplenishDecisionAppService` | application |
| 应用服务（读） | `{聚合根名}QueryAppService` / `Impl` | `StyleQueryAppService` | application |
| 应用服务（计算） | `{动词}QueryAppService` / `Impl` | `SizeAllocationQueryAppService` | application |
| 组装器 | `{聚合根名}Assembler` 或 `{场景}Assembler` | `ReplenishAssembler` | application |
| 请求 DTO | `{方法名}RequestDTO` | `OpenDecisionRequestDTO` | client |
| 响应 DTO | `{方法名}ResponseDTO` | `OpenDecisionResponseDTO` | client |
| Controller | `{业务名}Controller` | `ReplenishController` | adaptor/input |
| MQ 消费者 | `{业务名}MessageConsumer` | `SalesEventMessageConsumer` | adaptor/input |
| 定时任务 | `{业务名}ScheduleTask` | `CapacityRecycleScheduleTask` | adaptor/input |
| 出口适配 | `{外部系统}Adaptor` / `Impl` | `FactoryOrderAdaptor` | adaptor/output |

### 7.1 方法命名

| 位置 | 要求 | 正例 | 反例 |
|---|---|---|---|
| 聚合根/实体 | 动词，表业务意图 | `approve`、`changeStage` | `update`、`setStatus` |
| DomainService | 业务动词 | `calculateQty`、`openDecision` | `handle`、`doProcess` |
| AppService | 业务动词 | `approveDecision` | `save`、`execute` |
| Repository | 动词 | `save`、`query`、`buildLock` | `getData`、`doQuery` |
| Mapper | 动词+意图 | `selectById`、`updateStatusById` | `query` |

---

## 8. 编码约定

### 8.1 统一返回

所有 AppService / QueryAppService / DomainService / Repository 方法返回 `ResultDO<T>`，**禁止**向上抛未捕获异常。

```java
return ResultDO.buildSuccessResult(dto);
return ResultDO.buildFailResult("DECISION_DUPLICATED", "该款已存在进行中的决策单");
```

### 8.2 参数自校验

校验逻辑写在 DTO 自己的 `check()` 里，AppService 只负责调用，不写 if 判断。

```java
ResultDO checkResult = requestDTO.check();
if (!checkResult.isSuccess()) {
    return ResultDO.buildFailResult(checkResult.getCode(), checkResult.getMsg());
}
```

### 8.3 异常

| 异常 | 抛出位置 | 捕获位置 |
|---|---|---|
| `AggregateException` | 聚合根、实体内部校验失败 | DomainService |
| `BizException` | DomainService 业务校验失败 | AppService |
| `Throwable` | — | DomainService 必须兜住并记日志 |

```java
try {
    // ...
} catch (BizException e) {
    log.error("开决策单业务异常, param: {}", param, e);
    return ResultDO.buildFailResult(e.getCode(), e.getMsg());
} catch (Throwable e) {
    log.error("开决策单系统异常, param: {}", param, e);
    return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
}
```

### 8.4 实体属性必须包一层 `Field`

`Entity` 的属性禁止裸类型，统一用 `Field<T>` / `FieldSet<T>` / `FieldList<T>`，便于变更追踪。

```java
private Field<Integer> availableStock;
```

### 8.5 序列化

聚合根只以"快照对象"（`XxxResult`）跨出领域层，聚合根本身不参与 JSON 序列化。
如果某个场景确实要序列化聚合根，计算类/查找类/判断类方法必须显式排除
（本工程未引入 Lombok，故不使用 `@JSONField` 之类的注解，改用"快照对象"方案）。

### 8.6 DTO 存放位置

| 场景 | 位置 |
|---|---|
| 对外接口透传 | `client/{ctx}/req`、`client/{ctx}/res` |
| 仅 Application 内部流转 | `application/{ctx}/model` |

### 8.7 注释

简洁，只写"为什么"，不写"是什么"。方法上的 Javadoc 写业务语义，一行够。

---

## 9. 模式骨架（照抄）

### 9.1 写模式

```java
@Service
public class ReplenishDomainServiceImpl implements ReplenishDomainService {

    private static final Logger log = LoggerFactory.getLogger(ReplenishDomainServiceImpl.class);

    private final ReplenishDecisionRepository replenishDecisionRepository;

    public ReplenishDomainServiceImpl(ReplenishDecisionRepository replenishDecisionRepository) {
        this.replenishDecisionRepository = replenishDecisionRepository;
    }

    @Override
    public ResultDO<ReplenishDecisionResult> openDecision(OpenDecisionParam param) {
        LevelLock lock = replenishDecisionRepository.buildLock("style:decide:" + param.getStyleId());
        try {
            if (!lock.tryLock()) {
                return ResultDO.buildFailResult("LOCK_FAIL", "操作过于频繁，请稍后重试");
            }

            // 第一层幂等：已有进行中的单就直接返回它
            ResultDO<ReplenishDecisionAggregate> activeResult =
                    replenishDecisionRepository.query(DecisionQuery.activeByStyle(param.getStyleId()));
            if (!activeResult.isSuccess()) {
                return ResultDO.buildFailResult(activeResult.getMsg());
            }
            if (activeResult.getData() != null) {
                return ResultDO.buildSuccessResult(toResult(activeResult.getData(), true));
            }

            // 轮次必须在锁内取，否则两路并发会拿到同一个 round
            param.setRound(replenishDecisionRepository.nextRound(param.getStyleId()).getData());
            ReplenishDecisionAggregate decision = ReplenishDecisionAggregate.open(param);
            ResultDO<Void> saveResult = replenishDecisionRepository.save(decision);

        } catch (BizException e) {
            log.error("开决策单业务异常, param: {}", param, e);
            return ResultDO.buildFailResult(e.getCode(), e.getMsg());
        } catch (Throwable e) {
            log.error("开决策单系统异常, param: {}", param, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        } finally {
            lock.unlock();
        }
    }
}
```

### 9.2 规则+计算模式

```java
public class ReplenishRuleAggregate extends BaseAggregate<Long> {

    private ReplenishRuleEntity ruleEntity;

    /** 匹配款（无副作用，不修改状态） */
    public boolean matchStyle(MatchStyleParam param) {
        return this.ruleEntity.matchStyle(param);
    }

    /** 计算追单量（无副作用，不修改状态） */
    public CalculateQtyResult calculateQty(CalculateQtyParam param) {
        return this.ruleEntity.calculateQty(param);
    }
}
```

```java
@Service
public class ReplenishCalculateDomainServiceImpl implements ReplenishCalculateDomainService {

    private static final Logger log = LoggerFactory.getLogger(ReplenishCalculateDomainServiceImpl.class);

    private final ReplenishRuleRepository replenishRuleRepository;

    public ReplenishCalculateDomainServiceImpl(ReplenishRuleRepository replenishRuleRepository) {
        this.replenishRuleRepository = replenishRuleRepository;
    }

    @Override
    public ResultDO<CalculateQtyResult> calculateQty(CalculateQtyParam param) {
        try {
            // 仓储实现内部走倒排索引 + 位图交集，返回的已是精确命中的候选集
            ResultDO<List<ReplenishRuleAggregate>> ruleResult =
                replenishRuleRepository.queryMatchedRules(param.getMatchStyleParam());
            if (!ruleResult.isSuccess()) {
                return ResultDO.buildFailResult(ruleResult.getMsg());
            }

            List<ReplenishRuleAggregate> rules = ruleResult.getData();
            if (CollectionUtils.isEmpty(rules)) {
                return ResultDO.buildFailResult("RULE_IS_EMPTY", "无可用返单规则");
            }
            rules.sort(Comparator.comparingInt(ReplenishRuleAggregate::getPriority).reversed());

            for (ReplenishRuleAggregate rule : rules) {
                if (rule.matchStyle(param.getMatchStyleParam())) {
                    return ResultDO.buildSuccessResult(rule.calculateQty(param));
                }
            }
            return ResultDO.buildFailResult("NO_RULE_MATCHED", "无命中规则");

        } catch (AggregateException e) {
            log.error("追单量计算聚合根异常, param: {}", param, e);
            return ResultDO.buildFailResult(e.getCode(), e.getMsg());
        } catch (Throwable e) {
            log.error("追单量计算系统异常, param: {}", param, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }
}
```

### 9.3 DomainService 懒加载外部数据

领域层要外部数据（如尺码曲线、日历），**禁止依赖 Adaptor**，改为在 Param 里放函数式接口，由 Application 注入实现。

```java
// domain: 定义函数式接口
@FunctionalInterface
public interface SizeCurveProvider {
    SizeCurveValue getSizeCurve(Long styleId);
}

// domain: Param 持有
private SizeCurveProvider sizeCurveProvider;

// application: 注入实现
param.setSizeCurveProvider(styleId -> sizeCurveAdaptor.querySizeCurve(styleId));
```

### 9.4 纯计算模式

```java
@Service
public class SizeAllocationDomainServiceImpl implements SizeAllocationDomainService {

    private static final Logger log = LoggerFactory.getLogger(SizeAllocationDomainServiceImpl.class);

    @Override
    public ResultDO<SizeAllocationResult> allocate(SizeAllocationParam param) {
        try {
            // 权重 → 初分配 → 最大余额法收敛 → MOQ 收敛
            // 无聚合根、无仓储、无状态
            ...
        } catch (Throwable e) {
            log.error("尺码拆量系统异常, param: {}", param, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }
}
```

---

## 10. 关键约束速查（提交前自查）

| # | 检查项 | 违反后果 |
|---|---|---|
| 1 | 新代码是否落在 §5.2 映射表指定的模式与层？ | 架构走形，评审打回 |
| 2 | `domain` 里有没有 `RedisTemplate` / `RoaringBitmap` / `Caffeine` / HTTP 调用？ | 必须移出 |
| 3 | `domain` 里有没有 Strategy / Factory / 状态模式？ | 改回 `if/else` |
| 4 | `client` 有没有依赖 `model`？ | 必须自包含 |
| 5 | `application` 有没有直连 DB / MQ？ | 改走 Repository |
| 6 | 方法名是不是技术动词（`update`/`save`/`process`）？ | 改成业务动词 |
| 7 | 返回值是不是 `ResultDO`？有没有向上抛异常？ | 改 |
| 8 | 决策单写入有没有走三层幂等（锁 + CAS + 唯一键）？ | 必须补 |
| 9 | 产能变更有没有走 Lua 原子预占？ | 必须走 |
| 10 | 新增指标有没有落 Prometheus 或 `t_metric_minute`？ | 补 |
| 11 | 是否擅自新增了上下文或跨域依赖？ | 先问 |

---

## 11. 测试要求

| 测试类型 | 覆盖对象 | 要求 |
|---|---|---|
| 单元测试 | 聚合根方法、DomainService 计算逻辑 | 分支全覆盖（合法流转 + 所有非法流转） |
| 并发测试 | 决策单幂等、产能预占 | 三路并发触发，断言重复数 = 0、超占数 = 0 |
| 集成测试 | Repository 实现、Lua 脚本 | 真实 Redis + 内存 MySQL |
| 对账测试 | Redis 与 DB 一致性 | 注入差异，断言对账能发现并修正 |
| 压测 | 诊断链路 | JMeter 千级款 × 百万事件，出 P99 |

**必测的边界**：总量守恒（拆量）、MOQ 下界、水位乱序、水位重复、产能余量恰好相等、决策单并发。

---

## 12. 参考与维护

| 来源 | 用途 |
|---|---|
| [easy-DDD · DDD.md](https://github.com/alizhangsan602-bit/easy-DDD/blob/main/DDD.md) | 架构总览、四种模式、模式选择决策树 |
| [ddd-domain-layer.md](https://github.com/alizhangsan602-bit/easy-DDD/blob/main/ddd-domain-layer.md) | 聚合根/实体/值对象/Param/Result/DomainService/Repository |
| [ddd-application-layer.md](https://github.com/alizhangsan602-bit/easy-DDD/blob/main/ddd-application-layer.md) | 场景编排、Assembler、CQRS、懒加载注入 |
| [ddd-adaptor-layer.md](https://github.com/alizhangsan602-bit/easy-DDD/blob/main/ddd-adaptor-layer.md) | Input/Output/Inner 适配、技术适配可用设计模式 |
| [ddd-infrastructure-layer.md](https://github.com/alizhangsan602-bit/easy-DDD/blob/main/ddd-infrastructure-layer.md) | RepositoryImpl、PO、Mapper、Converter |
| [ddd-client-layer.md](https://github.com/alizhangsan602-bit/easy-DDD/blob/main/ddd-client-layer.md) | 对外接口定义、DTO 自包含 |
| [ddd-model-layer.md](https://github.com/alizhangsan602-bit/easy-DDD/blob/main/ddd-model-layer.md) | 共享枚举与通用业务概念 |

核对日期：2026-11-20。上游规范变更时，以本文件 §3–§9 的项目化裁剪为准；与本文件冲突的上游条目，先提出来讨论，不要自行改动本文件。