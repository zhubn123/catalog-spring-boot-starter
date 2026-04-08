package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogPage;
import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogRel;
import io.github.zhubn123.catalog.domain.CatalogTreeNode;
import io.github.zhubn123.catalog.exception.CatalogException;
import io.github.zhubn123.catalog.mapper.CatalogNodeMapper;
import io.github.zhubn123.catalog.mapper.CatalogRelMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 负责目录查询逻辑。
 *
 * <p>返回值统一保持为“按树遍历顺序排列的扁平节点列表”，由上层按需组装嵌套树。</p>
 */
final class CatalogQueryService {

    private static final Long ROOT_PARENT_ID = 0L;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private final CatalogNodeMapper nodeMapper;
    private final CatalogRelMapper relMapper;
    private final CatalogTreeAssembler treeAssembler;

    CatalogQueryService(CatalogNodeMapper nodeMapper, CatalogRelMapper relMapper) {
        this(nodeMapper, relMapper, new CatalogTreeAssembler());
    }

    CatalogQueryService(CatalogNodeMapper nodeMapper, CatalogRelMapper relMapper, CatalogTreeAssembler treeAssembler) {
        this.nodeMapper = nodeMapper;
        this.relMapper = relMapper;
        this.treeAssembler = treeAssembler;
    }

    List<CatalogNode> listNodesInTreeOrder() {
        return sortNodesForTreeTraversal(nodeMapper.selectAll());
    }

    List<CatalogNode> listChildrenNodes(Long parentId) {
        return nodeMapper.selectByParentId(normalizeParentId(parentId));
    }

    CatalogPage<CatalogNode> pageChildrenNodes(Long parentId, Integer page, Integer size) {
        int effectivePage = normalizePage(page);
        int effectiveSize = normalizePageSize(size);
        Long effectiveParentId = normalizeParentId(parentId);
        long total = nodeMapper.countByParentId(effectiveParentId);
        if (total == 0) {
            return new CatalogPage<>(effectivePage, effectiveSize, 0, false, Collections.emptyList());
        }

        long offset = (long) (effectivePage - 1) * effectiveSize;
        List<CatalogNode> items = nodeMapper.selectByParentIdPage(effectiveParentId, offset, effectiveSize);
        boolean hasNext = offset + items.size() < total;
        return new CatalogPage<>(effectivePage, effectiveSize, total, hasNext, items);
    }

    List<CatalogTreeNode> listNodeTree() {
        return treeAssembler.assemble(listNodesInTreeOrder(), CatalogTreeAssembleContext.fullTree());
    }

    List<CatalogNode> listPathByNodeId(Long nodeId) {
        CatalogNode node = nodeMapper.selectById(nodeId);
        if (node == null || !StringUtils.hasText(node.getPath())) {
            return Collections.emptyList();
        }

        List<Long> pathIds = Arrays.stream(node.getPath().split("/"))
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .toList();
        if (pathIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, CatalogNode> nodeMap = nodeMapper.selectByIds(pathIds)
                .stream()
                .collect(Collectors.toMap(CatalogNode::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        List<CatalogNode> result = new ArrayList<>(pathIds.size());
        for (Long pathId : pathIds) {
            CatalogNode pathNode = nodeMap.get(pathId);
            if (pathNode != null) {
                result.add(pathNode);
            }
        }
        return result;
    }

    List<String> getBizIds(Long nodeId, String bizType) {
        return relMapper.selectByNode(nodeId, bizType)
                .stream()
                .map(CatalogRel::getBizId)
                .toList();
    }

    List<String> getBizIdsByNodeTree(Long nodeId, String bizType) {
        CatalogNode node = nodeMapper.selectById(nodeId);
        if (node == null || !StringUtils.hasText(node.getPath())) {
            return Collections.emptyList();
        }

        List<CatalogNode> nodes = nodeMapper.selectByPathPrefix(node.getPath());
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> nodeIds = nodes.stream()
                .map(CatalogNode::getId)
                .toList();
        if (nodeIds.isEmpty()) {
            return Collections.emptyList();
        }

        return relMapper.selectBizIdsByNodeIds(nodeIds, bizType);
    }

    List<CatalogNode> listBizRelatedNodes(Long boundNodeId) {
        Set<Long> relatedNodeIds = new LinkedHashSet<>();
        CatalogNode node = nodeMapper.selectById(boundNodeId);
        if (node == null || !StringUtils.hasText(node.getPath())) {
            return Collections.emptyList();
        }

        relatedNodeIds.add(boundNodeId);
        List<Long> ancestorIds = Arrays.stream(node.getPath().split("/"))
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .toList();
        relatedNodeIds.addAll(ancestorIds);

        if (relatedNodeIds.isEmpty()) {
            return Collections.emptyList();
        }

        return sortNodesForTreeTraversal(nodeMapper.selectByIds(new ArrayList<>(relatedNodeIds)));
    }

    List<CatalogTreeNode> listBizRelatedTree(Long boundNodeId, String bizId, String bizType) {
        return treeAssembler.assemble(listBizRelatedNodes(boundNodeId), CatalogTreeAssembleContext.bizTree(bizId, bizType));
    }

    List<CatalogNode> listSubtreeNodes(Long nodeId) {
        CatalogNode node = nodeMapper.selectById(nodeId);
        if (node == null) {
            return Collections.emptyList();
        }
        if (!StringUtils.hasText(node.getPath())) {
            return List.of(node);
        }
        return sortNodesForTreeTraversal(nodeMapper.selectByPathPrefix(node.getPath()));
    }

    List<CatalogTreeNode> listSubtreeTree(Long nodeId) {
        return treeAssembler.assemble(listSubtreeNodes(nodeId), CatalogTreeAssembleContext.subtree(nodeId));
    }

    /**
     * 将节点列表重排为稳定的树前序遍历结果。
     *
     * <p>返回顺序遵循两条规则：</p>
     * <p>1. 同级节点始终按 {@code sort} 升序排列；</p>
     * <p>2. 父节点之后紧跟其子树，适合直接用于树形展示和遍历。</p>
     *
     * <p>这样可以避免继续依赖 {@code /1/10}、{@code /1/2} 这类 path 字符串排序带来的错序问题。</p>
     */
    private List<CatalogNode> sortNodesForTreeTraversal(List<CatalogNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, CatalogNode> nodeById = nodes.stream()
                .filter(Objects::nonNull)
                .filter(node -> node.getId() != null)
                .collect(Collectors.toMap(
                        CatalogNode::getId,
                        node -> node,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        if (nodeById.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<CatalogNode>> childrenByParentId = new LinkedHashMap<>();
        List<CatalogNode> roots = new ArrayList<>();
        for (CatalogNode node : nodeById.values()) {
            Long parentId = normalizeParentId(node.getParentId());
            // 子树查询或业务局部树查询往往只拿到局部节点集合，祖先节点未必完整。
            // 这种情况下，把缺失父节点的节点视为当前结果集中的“本地根节点”，
            // 可以保证返回顺序仍然可遍历、可展示。
            if (Objects.equals(parentId, ROOT_PARENT_ID) || !nodeById.containsKey(parentId)) {
                roots.add(node);
                continue;
            }
            childrenByParentId.computeIfAbsent(parentId, key -> new ArrayList<>()).add(node);
        }

        Comparator<CatalogNode> comparator = Comparator
                .comparingInt((CatalogNode node) -> defaultSort(node.getSort()))
                .thenComparing(node -> node.getId() == null ? Long.MAX_VALUE : node.getId());

        List<CatalogNode> ordered = new ArrayList<>(nodeById.size());
        appendNodesInTreeOrder(roots, childrenByParentId, comparator, ordered);
        return ordered;
    }

    /**
     * 递归按前序遍历顺序追加节点。
     */
    private void appendNodesInTreeOrder(
            List<CatalogNode> currentLevel,
            Map<Long, List<CatalogNode>> childrenByParentId,
            Comparator<CatalogNode> comparator,
            List<CatalogNode> ordered
    ) {
        if (currentLevel == null || currentLevel.isEmpty()) {
            return;
        }

        currentLevel.stream()
                .sorted(comparator)
                .forEach(node -> {
                    ordered.add(node);
                    appendNodesInTreeOrder(childrenByParentId.get(node.getId()), childrenByParentId, comparator, ordered);
                });
    }

    private Long normalizeParentId(Long parentId) {
        if (parentId == null || parentId <= 0) {
            return ROOT_PARENT_ID;
        }
        return parentId;
    }

    private int defaultSort(Integer sort) {
        return sort == null || sort <= 0 ? 1 : sort;
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page <= 0) {
            throw CatalogException.invalidArgument("page 必须大于 0");
        }
        return page;
    }

    private int normalizePageSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size <= 0) {
            throw CatalogException.invalidArgument("size 必须大于 0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw CatalogException.invalidArgument("size 不能大于 " + MAX_PAGE_SIZE);
        }
        return size;
    }
}
