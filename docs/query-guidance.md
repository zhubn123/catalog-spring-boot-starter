# 目录查询建议

这份文档用于说明：哪些目录查询接口更适合作为生产环境中的默认入口，哪些接口更适合围绕明确范围做局部查询。

## 推荐默认入口

生产环境优先使用下面两个接口：

- `GET /catalog/childrenPage`
- `GET /catalog/children`

推荐方式如下：

- 根节点列表默认从 `childrenPage` 进入。
- 非根节点按层懒加载时，优先使用 `children`。
- 当某个父节点下兄弟节点数量可能变大时，也优先切换到 `childrenPage`。

这两个接口每次只返回某一个父节点下的直接子节点，更适合以下场景：

- 树组件按层懒加载
- 根节点数量较多
- 某个热点父节点下兄弟节点很多
- 需要分页和渐进式渲染

## 聚焦型树查询

当调用方已经明确知道目标业务对象或节点范围时，可以优先考虑下面这些接口：

- `GET /catalog/bizPath`
- `GET /catalog/bizTree`
- `GET /catalog/bizTreeNodes`
- `GET /catalog/subtree`
- `GET /catalog/subtreeNodes`

这类接口通常比全量树查询更合适，因为它们只返回某一段局部树或某一类业务相关的目录结果。

## 已移除的全量读取接口

下面两个接口已经移除，不再建议作为默认目录读取方式：

- `GET /catalog/tree`
- `GET /catalog/nodes`

如果业务确实需要完整树快照，建议按范围改用 `bizTree` / `subtree`，或者在前端自行分批加载并维护树状态。

## 使用建议

- 默认从 `childrenPage` 读取根节点，再结合 `children` 做逐层导航。
- `children` 仅接受正数 `parentId`；根节点请不要再通过 `children` 查询。
- 只有在已经明确范围时，才使用 `bizTree`、`subtree` 这类局部树接口。
- 不要把完整目录快照当作生产环境中的默认读取路径。
