# Catalog Spring Boot Starter

通用目录树管理组件 - 开箱即用的 Spring Boot Starter

## ✨ 特性

- 🌳 **树形结构管理** - 支持无限层级的目录树
- 🔗 **业务对象绑定** - 叶子节点可选的一对一业务绑定
- 🎯 **拖拽排序** - 支持节点移动和排序
- 🚀 **高性能查询** - 路径冗余设计，避免递归查询
- 🔧 **开箱即用** - Spring Boot Starter 一键集成

## 📦 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.zhubn123</groupId>
    <artifactId>catalog-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 创建数据库表

```sql
-- 目录节点表
CREATE TABLE catalog_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50),
    path VARCHAR(500),
    level INT,
    sort INT,
    INDEX idx_parent (parent_id),
    INDEX idx_path (path)
);

-- 业务绑定关系表
CREATE TABLE catalog_rel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    node_id BIGINT NOT NULL,
    biz_id VARCHAR(100) NOT NULL,
    biz_type VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_biz (biz_id, biz_type),
    INDEX idx_node_type (node_id, biz_type),
    INDEX idx_biz (biz_id, biz_type)
);
```

> 说明：
> - 目录节点可以只作为容器，不必绑定业务对象
> - 只有叶子节点允许绑定业务对象
> - 同一 `biz_type + biz_id` 最多绑定一个目录节点

### 3. 配置 application.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_db
    username: root
    password: root

mybatis:
  mapper-locations: classpath:mapper/*.xml
```

### 4. 使用服务

```java
@Autowired
private CatalogService catalogService;

// 创建目录
Long projectId = catalogService.addNode(0L, "我的项目");
Long contractId = catalogService.addNode(projectId, "合同A");
Long deliveryId = catalogService.addNode(contractId, "交付物A");

// 目录节点可以只做容器，不必绑定业务对象
// 叶子节点可选绑定业务对象
catalogService.bind(deliveryId, "DELIVER-001", "deliver");

// 查询业务路径（单个业务对象最多绑定一个目录节点）
List<CatalogNode> path = catalogService.getBizPath("DELIVER-001", "deliver");
```

## 📖 API 文档

### 节点操作

| API | 方法 | 说明 |
|-----|------|------|
| `/catalog/node` | POST | 创建节点 |
| `/catalog/node/batch` | POST | 批量创建 |
| `/catalog/move` | POST | 移动节点 |
| `/catalog/node/update` | POST | 更新节点 |
| `/catalog/node/delete` | POST | 删除节点 |

### 业务绑定

| API | 方法 | 说明 |
|-----|------|------|
| `/catalog/bind` | POST | 绑定业务对象 |
| `/catalog/bind/batch` | POST | 兼容接口，仅允许单业务对象绑定单个节点 |
| `/catalog/bind/pairs` | POST | 批量一对一绑定 |
| `/catalog/unbind` | POST | 解除绑定 |

### 查询

| API | 方法 | 说明 |
|-----|------|------|
| `/catalog/nodes` | GET | 获取完整目录的扁平节点列表 |
| `/catalog/tree` | GET | 兼容旧接口，仍返回扁平节点列表 |
| `/catalog/bizPath` | GET | 查询业务路径 |
| `/catalog/bizTreeNodes` | GET | 查询业务局部树对应的扁平节点列表 |
| `/catalog/bizTree` | GET | 兼容旧接口，仍返回扁平节点列表 |
| `/catalog/subtreeNodes` | GET | 查询指定节点子树的扁平节点列表 |
| `/catalog/subtree` | GET | 兼容旧接口，仍返回扁平节点列表 |

> `nodes`、`bizTreeNodes`、`subtreeNodes` 返回的都是按树遍历顺序排列的扁平节点列表，
> 如果调用方需要真正的嵌套树结构，请在前端或上层服务中自行组装。

## 🔧 配置项

```yaml
catalog:
  enabled: true              # 是否启用
  enable-rest-api: true      # 是否启用REST API
```

> 当前公开配置仅保留已落地生效的选项；`table-prefix`、`init-schema` 等历史占位配置已移除。

## 🏗️ 项目结构

```
catalog-spring-boot-starter/
├── catalog-core/                    # 核心模块
│   ├── domain/                      # 领域模型
│   ├── service/                     # 服务接口和实现
│   ├── mapper/                      # 数据访问层
│   └── exception/                   # 异常定义
├── catalog-spring-boot-autoconfigure/  # 自动配置
└── catalog-spring-boot-starter/     # Starter聚合
```

## 📝 核心设计

### 业务绑定约束

- 目录节点可以只作为容器，不必绑定业务对象
- 只有叶子节点（无子节点）才能绑定业务对象
- 同一 `bizType + bizId` 最多绑定一个目录节点
- 批量绑定推荐使用“多组一对一绑定”，而不是让一个业务对象绑定多个节点

### 路径冗余设计

```
节点路径：/1/2/3
- 查询子树：WHERE path LIKE '/1/2/%'
- 查询祖先：解析 path 中的 ID 列表
```

## 📄 License

Apache License 2.0
