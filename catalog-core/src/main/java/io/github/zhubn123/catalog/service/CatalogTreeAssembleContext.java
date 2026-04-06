package io.github.zhubn123.catalog.service;

/**
 * 目录树组装上下文。
 *
 * <p>用于在组装嵌套树时向扩展策略传递当前场景信息，避免后续需要给
 * {@code tree}/{@code bizTree}/{@code subtree} 分别重做一套接口。</p>
 */
public record CatalogTreeAssembleContext(
        CatalogTreeScene scene,
        Long rootNodeId,
        String bizId,
        String bizType
) {

    public static CatalogTreeAssembleContext fullTree() {
        return new CatalogTreeAssembleContext(CatalogTreeScene.FULL_TREE, null, null, null);
    }

    public static CatalogTreeAssembleContext subtree(Long rootNodeId) {
        return new CatalogTreeAssembleContext(CatalogTreeScene.SUBTREE, rootNodeId, null, null);
    }

    public static CatalogTreeAssembleContext bizTree(String bizId, String bizType) {
        return new CatalogTreeAssembleContext(CatalogTreeScene.BIZ_TREE, null, bizId, bizType);
    }
}
