package io.github.zhubn123.catalog.domain;

/**
 * 排序修复结果。
 *
 * <p>用于显式治理目录排序数据，说明本次修复覆盖范围和实际更新数量。</p>
 */
public class CatalogSortRepairResult {

    private final String scope;
    private final Long parentId;
    private final int groups;
    private final int scannedNodes;
    private final int updatedNodes;

    public CatalogSortRepairResult(String scope, Long parentId, int groups, int scannedNodes, int updatedNodes) {
        this.scope = scope;
        this.parentId = parentId;
        this.groups = groups;
        this.scannedNodes = scannedNodes;
        this.updatedNodes = updatedNodes;
    }

    public String getScope() {
        return scope;
    }

    public Long getParentId() {
        return parentId;
    }

    public int getGroups() {
        return groups;
    }

    public int getScannedNodes() {
        return scannedNodes;
    }

    public int getUpdatedNodes() {
        return updatedNodes;
    }
}
