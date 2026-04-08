# 目录查询建议

这份文档用于说明：哪些目录查询接口更适合 sample 或后台管理页面，哪些接口更适合作为生产环境中的默认查询入口。

## 推荐默认入口

生产环境优先使用下面两个接口：

- `GET /catalog/children`
- `GET /catalog/childrenPage`

这两个接口每次只返回某一个父节点下的直接子节点，更适合以下场景：

- 树组件按层懒加载
- 根节点数量较多
- 某个热点父节点下兄弟节点很多
- 需要分页和渐进式渲染

## 更适合 sample 或后台的接口

下面这些接口仍然有价值，但不建议直接作为高频生产查询的默认入口：

- `GET /catalog/tree`
- `GET /catalog/nodes`

它们更适合：

- sample 或演示页面
- 树规模可控的后台管理界面
- 全量导出或排障
- 需要一次拿到完整目录快照的场景

## 聚焦型树查询

当调用方已经明确知道目标业务对象或节点范围时，可以优先考虑下面这些接口：

- `GET /catalog/bizPath`
- `GET /catalog/bizTree`
- `GET /catalog/bizTreeNodes`
- `GET /catalog/subtree`
- `GET /catalog/subtreeNodes`

这类接口通常比全量树查询更合适，因为它们只返回某一段局部树或某一类业务相关的目录结果。

## 使用建议

- 默认从 `children` 开始做增量导航。
- 当某个父节点下兄弟节点数量可能变大时，优先切换到 `childrenPage`。
- 只有在确实需要完整嵌套树时，才使用 `tree`。
- 不要把全量树查询当作生产环境中的默认读取路径。
