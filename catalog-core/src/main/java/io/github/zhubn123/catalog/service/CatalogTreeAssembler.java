package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogTreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 目录树组装器。
 *
 * <p>输入为已经按树遍历顺序排好的扁平节点列表，输出为可直接序列化的嵌套树结构。
 * 这样可以同时保留“扁平列表查询”和“树形查询”两种返回形式，而无需重复维护排序逻辑。</p>
 */
final class CatalogTreeAssembler {

    private static final Long ROOT_PARENT_ID = 0L;

    List<CatalogTreeNode> assemble(List<CatalogNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, CatalogTreeNode> treeNodeById = new LinkedHashMap<>();
        for (CatalogNode node : nodes) {
            if (node == null || node.getId() == null || treeNodeById.containsKey(node.getId())) {
                continue;
            }
            treeNodeById.put(node.getId(), CatalogTreeNode.from(node));
        }
        if (treeNodeById.isEmpty()) {
            return Collections.emptyList();
        }

        List<CatalogTreeNode> roots = new ArrayList<>();
        for (CatalogTreeNode treeNode : treeNodeById.values()) {
            Long parentId = normalizeParentId(treeNode.getParentId());
            CatalogTreeNode parent = treeNodeById.get(parentId);
            if (Objects.equals(parentId, ROOT_PARENT_ID) || parent == null) {
                roots.add(treeNode);
                continue;
            }
            parent.getChildren().add(treeNode);
        }
        return roots;
    }

    private Long normalizeParentId(Long parentId) {
        if (parentId == null || parentId <= 0) {
            return ROOT_PARENT_ID;
        }
        return parentId;
    }
}
