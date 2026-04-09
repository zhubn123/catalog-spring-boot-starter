# Catalog REST API Reference

这份文档面向直接接入 `catalog-spring-boot-starter` REST 接口的调用方，重点说明：

- 每个接口的用途与推荐调用方式
- 请求参数与请求体结构
- 扁平列表接口和嵌套树接口的区别
- 常见错误码与 HTTP 状态码

## 1. 基本约定

### 1.1 基础路径

所有 REST 接口默认挂载在：

```text
/catalog
```

### 1.2 写接口调用方式

所有 `POST` 接口都优先推荐使用 JSON 请求体。

当前为了兼容旧调用方，部分接口仍然兼容 query/form 参数；但新接入方建议统一按 JSON body 调用，避免 CSV 字符串和参数歧义。

### 1.3 业务绑定约束

- 目录节点可以只作为容器节点，不强制绑定业务对象。
- 默认绑定策略不区分叶子/非叶子节点。
- 同一个 `bizType + bizId` 最多只能绑定一个目录节点。
- 批量绑定推荐使用“多组一对一绑定”，而不是把同一个业务对象绑定到多个节点。

## 2. 响应模型

### 2.1 CatalogNode

扁平节点列表接口返回 `CatalogNode` 数组，字段如下：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `Long` | 节点 ID |
| `parentId` | `Long` | 父节点 ID，根节点通常为 `0` |
| `name` | `String` | 节点名称 |
| `code` | `String` | 业务编码，可为空 |
| `path` | `String` | 路径冗余字段，例如 `/1/2/3` |
| `level` | `Integer` | 节点层级 |
| `sort` | `Integer` | 同级排序值 |

### 2.2 CatalogTreeNode

嵌套树接口返回 `CatalogTreeNode` 数组。它在 `CatalogNode` 基础上额外提供：

| 字段 | 类型 | 说明 |
|------|------|------|
| `leaf` | `Boolean` | 当前返回树中的叶子节点标记 |
| `bindable` | `Boolean` | 当前节点是否适合绑定业务对象 |
| `extensions` | `Map<String, Object>` | 预留扩展信息 |
| `children` | `List<CatalogTreeNode>` | 子节点列表 |

sample 工程当前会在叶子节点补充一个 `extensions.bindingSummary`，结构大致如下：

```json
{
  "count": 2,
  "bizTypes": ["deliver"],
  "bizIds": ["DELIVER-101", "DELIVER-102"],
  "directBindings": [
    { "bizId": "DELIVER-101", "bizType": "deliver" },
    { "bizId": "DELIVER-102", "bizType": "deliver" }
  ],
  "scene": "FULL_TREE"
}
```

示例：

```json
[
  {
    "id": 1,
    "parentId": 0,
    "name": "项目A",
    "code": null,
    "path": "/1",
    "level": 1,
    "sort": 0,
    "leaf": false,
    "bindable": false,
    "extensions": {},
    "children": [
      {
        "id": 2,
        "parentId": 1,
        "name": "合同A",
        "code": null,
        "path": "/1/2",
        "level": 2,
        "sort": 0,
        "leaf": true,
        "bindable": true,
        "extensions": {},
        "children": []
      }
    ]
  }
]
```

## 3. 错误响应

统一错误响应结构：

```json
{
  "code": "INVALID_ARGUMENT",
  "message": "nodeId 不能为空且必须大于 0"
}
```

常见 HTTP 状态码：

| HTTP 状态码 | 适用场景 |
|-------------|----------|
| `400` | 参数错误、请求体格式错误、校验失败 |
| `404` | 节点不存在、父节点不存在 |
| `409` | 删除时存在子节点/绑定、业务对象已绑定到其他节点 |
| `500` | 未预期的服务端异常 |

常见错误码：

| 错误码 | 说明 |
|--------|------|
| `INVALID_REQUEST` | 请求体无法解析或参数类型错误 |
| `INVALID_ARGUMENT` | 参数缺失、为空、长度不一致、索引非法等 |
| `NODE_NOT_FOUND` | 节点不存在 |
| `PARENT_NOT_FOUND` | 父节点不存在 |
| `NAME_BLANK` | 节点名称为空 |
| `CANNOT_MOVE_TO_SELF` | 不能移动到自己或自己的子树下 |
| `HAS_CHILDREN` | 删除节点时仍有子节点 |
| `HAS_BINDINGS` | 删除节点时仍有业务绑定 |
| `BIZ_ALREADY_BOUND` | 业务对象已绑定到其他节点 |
| `BIZ_BOUND_TO_MULTIPLE_NODES` | 历史数据存在多节点绑定冲突 |

## 4. 节点操作

### 4.1 创建单个节点

`POST /catalog/node`

请求体：

```json
{
  "parentId": 1,
  "name": "合同A"
}
```

响应：

```json
2
```

说明：

- `parentId` 传 `0` 时表示创建根节点。
- `name` 必填。

### 4.2 批量创建同级节点

`POST /catalog/node/batch`

请求体：

```json
{
  "parentId": 2,
  "names": ["交付物A", "交付物B", "交付物C"]
}
```

响应：

```json
[11, 12, 13]
```

说明：

- 新节点会挂在同一个父节点下。
- 推荐使用 JSON 数组，不建议新调用方继续使用 `namesCsv` 风格参数。

### 4.3 移动节点

`POST /catalog/move`

请求体：

```json
{
  "nodeId": 12,
  "parentId": 2,
  "targetIndex": 0
}
```

说明：

- `parentId` 可用于跨父节点迁移。
- `targetIndex` 表示在新同级列表中的目标位置，从 `0` 开始。

### 4.4 更新节点

`POST /catalog/node/update`

请求体：

```json
{
  "nodeId": 12,
  "name": "交付物A-已签收",
  "code": "DELIVER-A",
  "sort": 1
}
```

说明：

- `name`、`code`、`sort` 三者至少提供一个。
- 提供 `sort` 时会复用节点移动排序逻辑调整同级顺序。

### 4.5 删除节点

`POST /catalog/node/delete`

请求体：

```json
{
  "nodeId": 12,
  "recursive": false
}
```

说明：

- `recursive=false` 时，会校验是否还有子节点或业务绑定。
- `recursive=true` 时，会连同整棵子树一起删除。

## 5. 业务绑定

### 5.1 绑定单个业务对象

`POST /catalog/bind`

请求体：

```json
{
  "nodeId": 12,
  "bizId": "DELIVER-001",
  "bizType": "deliver"
}
```

说明：

- 默认绑定策略不限制叶子/非叶子节点。
- 如果同一个业务对象已经绑定到其他节点，会返回 `409`。
- 如果重复绑定到同一个节点，会按幂等语义处理。

### 5.2 兼容批量绑定接口

`POST /catalog/bind/batch`

请求体：

```json
{
  "nodeIds": [12],
  "bizId": "DELIVER-001",
  "bizType": "deliver"
}
```

说明：

- 这是兼容旧调用方保留的接口。
- 当前单绑定语义下，只允许传入一个有效节点。
- 新接入方建议直接使用 `/catalog/bind` 或 `/catalog/bind/pairs`。

### 5.3 批量一对一绑定

`POST /catalog/bind/pairs`

请求体：

```json
{
  "nodeIds": [21, 22, 23],
  "bizIds": ["DELIVER-101", "DELIVER-102", "DELIVER-103"],
  "bizType": "deliver"
}
```

含义：

- `nodeIds[0] -> bizIds[0]`
- `nodeIds[1] -> bizIds[1]`
- `nodeIds[2] -> bizIds[2]`

说明：

- 两个数组长度必须一致。
- 适合“先批量建叶子节点，再按顺序绑定多个业务对象”的场景。

### 5.4 解除绑定

`POST /catalog/unbind`

请求体：

```json
{
  "nodeId": 12,
  "bizId": "DELIVER-001",
  "bizType": "deliver"
}
```

## 6. 查询接口

### 6.1 查询直接子节点

`GET /catalog/children?parentId=12`

说明：

- 返回 `List<CatalogNode>`。
- 仅接受正数 `parentId`。
- 根节点查询不再通过这个接口暴露，请改用 `/catalog/childrenPage`。

### 6.2 分页查询直接子节点

`GET /catalog/childrenPage?parentId=0&page=1&size=20`

说明：

- 返回 `CatalogPage<CatalogNode>`。
- `parentId <= 0` 时表示查询根节点分页结果。
- 推荐作为根节点列表和大兄弟集合的默认读取入口。

### 6.3 查询业务路径

`GET /catalog/bizPath?bizId=DELIVER-001&bizType=deliver`

说明：

- 返回该业务对象所在路径上的节点列表。
- 如果未绑定任何节点，返回空数组。
- 如果历史数据里同一个业务对象被绑定到了多个节点，会返回冲突错误而不是静默取第一条。

### 6.4 查询业务对象当前绑定的节点 ID

`GET /catalog/bizNodes?bizId=DELIVER-001&bizType=deliver`

说明：

- 在当前单绑定语义下，返回值通常为空数组或只包含一个节点 ID。
- 保留数组结构是为了兼容已有调用方。

### 6.5 查询当前节点的直接绑定

`GET /catalog/nodeBindings?nodeId=12&bizType=deliver`

说明：

- 返回当前节点直接绑定的业务 ID 列表。
- 不会向下递归子树。

### 6.6 查询当前节点整棵子树的业务 ID

`GET /catalog/nodeBiz?nodeId=2&bizType=deliver`

说明：

- 返回当前节点及其子树内所有绑定的业务 ID。
- 适合做某个目录分支下的业务集合查询。

### 6.7 查询业务局部树的扁平列表

`GET /catalog/bizTreeNodes?bizId=DELIVER-001&bizType=deliver`

说明：

- 返回绑定节点及其祖先节点构成的局部树。
- 结果是扁平列表，适合前端自行二次加工。

### 6.8 查询业务局部树的嵌套结构

`GET /catalog/bizTree?bizId=DELIVER-001&bizType=deliver`

说明：

- 返回绑定节点及其祖先节点构成的嵌套树。
- 适合面包屑、定位树、局部目录高亮等场景。

### 6.9 查询指定节点子树的扁平列表

`GET /catalog/subtreeNodes?nodeId=2`

### 6.10 查询指定节点子树的嵌套结构

`GET /catalog/subtree?nodeId=2`

说明：

- `subtreeNodes` 返回扁平列表。
- `subtree` 返回已组装的嵌套树。

## 7. 接口选择建议

### 7.1 前端树组件直接展示

优先使用：

- `/catalog/childrenPage` 作为根节点入口
- `/catalog/children` 作为非根节点懒加载入口
- `/catalog/bizTree`
- `/catalog/subtree`

### 7.2 前端自己维护树状态或做虚拟滚动

优先使用：

- `/catalog/childrenPage`
- `/catalog/children`
- `/catalog/bizTreeNodes`
- `/catalog/subtreeNodes`

说明：

- `/catalog/nodes` 与 `/catalog/tree` 这两个整棵树全量读取接口已移除。
- 如果需要完整树快照，建议按业务范围改用 `bizTree` / `subtree` 或自行分批加载。

### 7.3 创建目录但暂不绑定业务对象

只调用节点操作接口即可，不需要调用任何绑定接口。

### 7.4 批量创建叶子节点后再绑定业务对象

推荐流程：

1. 调用 `/catalog/node/batch` 批量建节点
2. 调用 `/catalog/bind/pairs` 做多组一对一绑定

## 8. 兼容接口说明

以下接口当前仍保留，但更适合作为兼容入口而不是新设计的首选：

| 接口 | 状态 | 说明 |
|------|------|------|
| `/catalog/bind/batch` | 兼容保留 | 旧批量绑定接口，当前仅接受单节点输入 |
| `/catalog/bizNodes` | 兼容保留 | 仍返回数组结构，但单绑定语义下通常只有 0 或 1 个元素 |
