package io.github.zhubn123.catalog.service.tree;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogTreeNode;
import io.github.zhubn123.catalog.service.CatalogTreeAssembleContext;
import io.github.zhubn123.catalog.service.CatalogTreeNodeEnricher;

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
public final class CatalogTreeAssembler {

    private static final Long ROOT_PARENT_ID = 0L;
    private final List<CatalogTreeNodeEnricher> enrichers;

    public CatalogTreeAssembler() {
        this(List.of());
    }

    public CatalogTreeAssembler(List<CatalogTreeNodeEnricher> enrichers) {
        this.enrichers = enrichers == null ? List.of() : List.copyOf(enrichers);
    }

    public List<CatalogTreeNode> assemble(List<CatalogNode> nodes) {
        return assemble(nodes, CatalogTreeAssembleContext.fullTree());
    }

    /**
     * 将扁平节点列表组装为嵌套树，并在组装完成后应用扩展策略。
     */
    public List<CatalogTreeNode> assemble(List<CatalogNode> nodes, CatalogTreeAssembleContext context) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, CatalogNode> sourceNodeById = new LinkedHashMap<>();
        Map<Long, CatalogTreeNode> treeNodeById = new LinkedHashMap<>();
        for (CatalogNode node : nodes) {
            if (node == null || node.getId() == null || treeNodeById.containsKey(node.getId())) {
                continue;
            }
            sourceNodeById.put(node.getId(), node);
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

        markLeafAndBindable(treeNodeById);
        applyEnrichers(treeNodeById, sourceNodeById, context);
        return roots;
    }

    /**
     * 统一补齐树节点的叶子标记和默认绑定能力。
     */
    private void markLeafAndBindable(Map<Long, CatalogTreeNode> treeNodeById) {
        for (CatalogTreeNode treeNode : treeNodeById.values()) {
            boolean leaf = treeNode.getChildren() == null || treeNode.getChildren().isEmpty();
            treeNode.setLeaf(leaf);
            treeNode.setBindable(true);
        }
    }

    /**
     * 先让每个增强器按整棵树做一次预取，再逐节点执行组装。
     *
     * <p>这样既兼容最简单的单节点增强写法，也能支持 sample 中这种“先批量取绑定关系，
     * 再逐节点填充摘要”的扩展模式，避免每个目录节点都单独查询一次。</p>
     */
    private void applyEnrichers(
            Map<Long, CatalogTreeNode> treeNodeById,
            Map<Long, CatalogNode> sourceNodeById,
            CatalogTreeAssembleContext context
    ) {
        if (enrichers.isEmpty()) {
            return;
        }
        CatalogTreeAssembleContext effectiveContext = context == null ? CatalogTreeAssembleContext.fullTree() : context;
        List<CatalogNode> sourceNodes = List.copyOf(sourceNodeById.values());
        List<Object> preparedStates = new ArrayList<>(enrichers.size());
        for (CatalogTreeNodeEnricher enricher : enrichers) {
            preparedStates.add(enricher.prepare(sourceNodes, effectiveContext));
        }

        for (CatalogTreeNode treeNode : treeNodeById.values()) {
            CatalogNode sourceNode = sourceNodeById.get(treeNode.getId());
            if (sourceNode == null) {
                continue;
            }
            for (int index = 0; index < enrichers.size(); index++) {
                enrichers.get(index).enrich(treeNode, sourceNode, effectiveContext, preparedStates.get(index));
            }
        }
    }

    private Long normalizeParentId(Long parentId) {
        if (parentId == null || parentId <= 0) {
            return ROOT_PARENT_ID;
        }
        return parentId;
    }
}
