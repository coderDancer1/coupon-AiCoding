# 优惠券管理系统（coupon-system）

基于 Spring Boot 3 + MyBatis-Plus + MySQL 的优惠券管理系统，支持优惠券管理（CRUD）与高并发领取，重点保证 **库存不超发**、**同一用户不重复领取**。

## 技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| Java | 17（编译目标，可用 JDK 17/21） | 语言 |
| Spring Boot | 3.3.4 | 基础框架 |
| MySQL | 8.x | 数据库 |
| MyBatis-Plus | 3.5.9 | ORM（含分页插件 mybatis-plus-jsqlparser） |
| Maven | 3.9.x | 构建 |
| Lombok | 由 Spring Boot BOM 管理 | 简化样板代码 |
| JUnit 5 | 由 Spring Boot BOM 管理 | 单元/集成测试 |

## 环境要求

- JDK 17+（`maven.compiler.release=17`，JDK 21 亦可编译出 Java 17 字节码）
- Maven 3.9+
- MySQL 8（默认 `localhost:3306`）

## 目录结构

```
src/main/java/com/coupon/
├── CouponApplication.java        # 启动类
├── controller/
│   ├── CouponController.java     # 优惠券管理接口（CRUD）
│   ├── CouponReceiveController.java  # 领取接口
│   └── HealthController.java     # 健康检查接口
├── service/
│   ├── CouponService.java        # 优惠券管理业务接口
│   └── CouponReceiveService.java # 领取业务接口
├── service/impl/
│   ├── CouponServiceImpl.java
│   └── CouponReceiveServiceImpl.java
├── mapper/
│   ├── CouponMapper.java         # 含原子扣库存、并发保护更新等自定义 SQL
│   └── UserCouponMapper.java
├── entity/
│   ├── Coupon.java               # 对应表 coupon（逻辑删除）
│   └── UserCoupon.java           # 对应表 user_coupon
├── dto/                          # 请求 DTO（Bean Validation）
│   ├── CouponCreateRequest.java
│   ├── CouponUpdateRequest.java
│   └── CouponQueryRequest.java
├── vo/
│   └── CouponVO.java             # 响应视图对象
├── common/
│   ├── Result.java               # 统一响应封装
│   └── ResultCode.java           # 状态码枚举
├── exception/
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
└── config/
    └── MybatisPlusConfig.java    # 分页插件

src/main/resources/
├── application.yml               # 主配置（密码走环境变量）
└── db/schema.sql                 # 建表脚本
```

## 快速开始

### 1. 创建数据库并建表

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS coupon_db DEFAULT CHARSET utf8mb4;"
mysql -uroot -p coupon_db < src/main/resources/db/schema.sql
```

### 2. 配置环境变量

数据库密码通过环境变量 `DB_PASSWORD` 注入，`application.yml` 中**不含明文密码**。

```bash
# Linux / macOS / Git Bash
export DB_PASSWORD=你的MySQL密码

# Windows CMD
set DB_PASSWORD=你的MySQL密码

# Windows PowerShell
$env:DB_PASSWORD="你的MySQL密码"
```

可选：数据库用户名默认 `root`，可通过 `DB_USERNAME` 覆盖。

### 3. 编译打包

```bash
mvn clean package
```

### 4. 启动

```bash
mvn spring-boot:run
# 或
java -jar target/coupon-system-1.0.0.jar
```

### 5. 验证

```bash
curl http://localhost:8080/api/ping
# {"code":200,"message":"操作成功","data":{"status":"UP","service":"coupon-system",...}}
```

## API 接口说明

### 统一响应结构

所有接口返回 `{ "code": 200, "message": "...", "data": ... }`，`code=200` 表示成功。业务错误通过 `code` 区分（如 `404` 资源不存在），HTTP 状态码恒为 200。

### 1. 创建优惠券

`POST /api/coupons`

```bash
curl -X POST http://localhost:8080/api/coupons \
  -H "Content-Type: application/json" \
  -d '{
    "name": "满100减20",
    "amount": 20.00,
    "totalCount": 1000,
    "validStartTime": "2026-01-01T00:00:00",
    "validEndTime": "2026-12-31T23:59:59"
  }'
```

校验规则：名称非空且 ≤64 字符；金额 ≥ 0；发行总量 > 0；开始时间早于结束时间。

### 2. 分页查询优惠券（支持名称筛选）

`GET /api/coupons?page=1&size=10&name=满`

### 3. 查询优惠券详情

`GET /api/coupons/{id}`

### 4. 修改优惠券

`PUT /api/coupons/{id}`（请求体同创建；发行总量不能小于已领取数量）

### 5. 删除优惠券（逻辑删除）

`DELETE /api/coupons/{id}`

### 6. 用户领取优惠券

`POST /api/coupons/{couponId}/receive?userId=1001`

成功返回领取记录 ID；失败返回具体原因（不存在 / 已删除 / 未生效 / 已过期 / 库存不足 / 重复领取）。

## 核心并发控制与事务设计

### 1. 库存不超发（原子条件更新）

领取时**不采用「先查库存再 Java 判断再更新」**，而是用一条条件更新原子扣减：

```sql
UPDATE coupon SET received_count = received_count + 1
WHERE id = ? AND received_count < total_count AND deleted = 0
  AND valid_start_time <= NOW() AND valid_end_time >= NOW();
```

`received_count < total_count` 是硬门槛，InnoDB 行锁下串行执行。100 个请求抢最后 1 张时，只有 1 条 UPDATE 影响行数为 1，其余 99 条为 0，**恰好 1 人成功，绝不超发**。

### 2. 防重复领取（唯一索引兜底）

`user_coupon` 表存在唯一索引 `uk_user_coupon(user_id, coupon_id)`。插入第二条记录时触发 `DuplicateKeyException`，捕获后抛出业务异常并回滚。**不依赖 Java 预查**。

### 3. 事务一致性

库存扣减与领取记录插入在**同一个 `@Transactional(rollbackFor = Exception.class)` 事务**内。唯一键冲突被捕获后**必须重新抛出**（而非吞掉），否则事务会提交，留下「库存已扣但无记录」。

### 4. 修改发行总量与并发领取的竞态

修改发行总量也用数据库层条件更新，而非「先查再判」：

```sql
UPDATE coupon SET total_count = ?, ...
WHERE id = ? AND deleted = 0 AND received_count <= ?;
```

领取（`received_count < total_count`）与修改总量（`received_count <= totalCount`）都是原子条件更新，在行锁下串行，任何顺序下都不可能产生 `received_count > total_count`。

## 测试

### 运行方式

需先设置 `DB_PASSWORD` 环境变量并确保 MySQL 运行：

```bash
export DB_PASSWORD=你的MySQL密码
mvn test
```

### 覆盖范围与结果

- `CouponServiceTest`（8 项）：CRUD 增删改查、参数校验、逻辑删除、总量校验。
- `CouponReceiveServiceTest`（10 项）：领取成功/失败各分支、**100 用户并发抢 1 张**、**同一用户并发领取**、插入失败回滚。

结果：**18 项测试全部通过**（`Tests run: 18, Failures: 0, Errors: 0`），基于真实 MySQL 集成测试，并发用例使用线程池 + CountDownLatch 真实并发。

## 安全说明

- 数据库密码通过环境变量 `DB_PASSWORD` 注入，代码与配置中**不含明文密码**。
- `.gitignore` 已忽略 `target/`、IDE 配置、日志，以及 `application-local.yml`、`.env` 等本地敏感文件。
