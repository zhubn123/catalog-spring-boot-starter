package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogRel;
import io.github.zhubn123.catalog.exception.CatalogException;
import io.github.zhubn123.catalog.mapper.CatalogNodeMapper;
import io.github.zhubn123.catalog.mapper.CatalogRelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * 目录领域服务实现。
 *
 * @author zhubn
 * @date 2026/4/2
 */
@Service
public class CatalogServiceImpl implements CatalogService {

    private static final Long ROOT_PARENT_ID = 0L;

    private final CatalogNodeMapper nodeMapper;
    private final CatalogRelMapper relMapper;

    public CatalogServiceImpl(CatalogNodeMapper nodeMapper, CatalogRelMapper relMapper) {
        this.nodeMapper = nodeMapper;
        this.relMapper = relMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addNode(Long parentId, String name) {
        Long normalizedParentId = normalizeParentId(parentId);
        String nodeName = trimToNull(name);
        if (nodeName == null) {
            throw CatalogException.nameBlank();
        }

        CatalogNode parent = getParentNode(normalizedParentId);
        normalizeSiblingSorts(normalizedParentId);

        CatalogNode node = new CatalogNode();
        node.setParentId(normalizedParentId);
        node.setName(nodeName);
        node.setLevel(parent == null ? 1 : parent.getLevel() + 1);
        node.setSort(nextAppendSort(normalizedParentId));

        nodeMapper.insert(node);

        String path = buildPath(parent, node.getId());
        nodeMapper.updatePath(node.getId(), path);

        return node.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchAddNode(Long parentId, String[] names) {
        if (names == null || names.length == 0) {
            return Collections.emptyList();
        }

        Long normalizedParentId = normalizeParentId(parentId);
        CatalogNode parent = getParentNode(normalizedParentId);

        normalizeSiblingSorts(normalizedParentId);

        List<String> validNames = Arrays.stream(names)
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .toList();
        if (validNames.isEmpty()) {
            return Collections.emptyList();
        }

        int startSort = nextAppendSort(normalizedParentId);

        List<CatalogNode> nodes = new ArrayList<>(validNames.size());
        for (int i = 0; i < validNames.size(); i++) {
            CatalogNode node = new CatalogNode();
            node.setParentId(normalizedParentId);
            node.setName(validNames.get(i));
            node.setLevel(parent == null ? 1 : parent.getLevel() + 1);
            node.setSort(startSort + i);
            nodes.add(node);
        }

        nodeMapper.batchInsert(nodes);

        for (CatalogNode node : nodes) {
            node.setPath(buildPath(parent, node.getId()));
        }
        nodeMapper.batchUpdatePath(nodes);

        return nodes.stream().map(CatalogNode::getId).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNode(Long nodeId, String name, String code, Integer sort) {
        CatalogNode node = getRequiredNode(nodeId);

        String normalizedName = trimToNull(name);
        String normalizedCode = trimToNull(code);
        if (normalizedName != null || normalizedCode != null) {
            nodeMapper.updateBasic(nodeId, normalizedName, normalizedCode);
        }

        if (sort != null) {
            moveNode(nodeId, node.getParentId(), sort);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveNode(Long nodeId, Long parentId) {
        moveNode(nodeId, parentId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveNode(Long nodeId, Long parentId, Integer targetIndex) {
        CatalogNode node = getRequiredNode(nodeId);

        Long oldParentId = normalizeParentId(node.getParentId());
        Long newParentId = normalizeParentId(parentId);

        normalizeSiblingSorts(oldParentId);
        if (!Objects.equals(oldParentId, newParentId)) {
            normalizeSiblingSorts(newParentId);
            node = getRequiredNode(nodeId);
        } else {
            node = getRequiredNode(nodeId);
        }

        CatalogNode newParent = getParentNode(newParentId);

        validateMoveTarget(node, newParentId, newParent);

        int oldSort = defaultSort(node.getSort());
        int newSort = resolveTargetSort(newParentId, targetIndex, oldParentId);

        if (Objects.equals(oldParentId, newParentId)) {
            if (newSort == oldSort) {
                return;
            }
            reorderWithinSameParent(nodeId, oldParentId, oldSort, newSort);
            return;
        }

        nodeMapper.decrementSortAfter(oldParentId, oldSort);
        nodeMapper.incrementSortFrom(newParentId, newSort);

        String newPath = buildPath(newParent, nodeId);
        int oldLevel = node.getLevel() == null ? 1 : node.getLevel();
        int parentLevel = newParent == null ? 0 : defaultLevel(newParent.getLevel());
        int newLevel = parentLevel + 1;
        int levelDelta = newLevel - oldLevel;

        nodeMapper.updateParentLevelPathSort(nodeId, newParentId, newLevel, newPath, newSort);
        nodeMapper.moveSubtree(node.getPath(), newPath, levelDelta);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNode(Long nodeId, boolean recursive) {
        CatalogNode node = getRequiredNode(nodeId);
        Long parentId = normalizeParentId(node.getParentId());

        normalizeSiblingSorts(parentId);
        node = getRequiredNode(nodeId);

        List<CatalogNode> subtree = nodeMapper.selectByPathPrefix(node.getPath());
        List<Long> nodeIds = subtree.stream()
                .map(CatalogNode::getId)
                .toList();
        if (nodeIds.isEmpty()) {
            return;
        }

        int bindingCount = countBindings(nodeIds);

        if (!recursive) {
            if (subtree.size() > 1) {
                throw CatalogException.hasChildren(nodeId);
            }
            if (bindingCount > 0) {
                throw CatalogException.hasBindings(nodeId);
            }
        }

        if (bindingCount > 0) {
            relMapper.deleteByNodeIds(nodeIds);
        }

        nodeMapper.deleteByIds(nodeIds);
        nodeMapper.decrementSortAfter(parentId, defaultSort(node.getSort()));
    }

    @Override
    public void bind(Long nodeId, String bizId, String bizType) {
        validateBindArgs(nodeId, bizId, bizType);
        ensureLeafNode(nodeId);

        String normalizedBizId = trimToNull(bizId);
        String normalizedBizType = trimToNull(bizType);
        Long existingNodeId = resolveSingleBoundNodeId(normalizedBizId, normalizedBizType);
        if (existingNodeId != null) {
            if (Objects.equals(existingNodeId, nodeId)) {
                return;
            }
            throw CatalogException.bizAlreadyBound(normalizedBizId, normalizedBizType, existingNodeId);
        }

        CatalogRel rel = new CatalogRel();
        rel.setNodeId(nodeId);
        rel.setBizId(normalizedBizId);
        rel.setBizType(normalizedBizType);
        relMapper.insert(rel);
    }

    @Deprecated
    @Override
    public void batchBind(List<Long> nodeIds, String bizId, String bizType) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return;
        }

        List<Long> validNodeIds = nodeIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (validNodeIds.isEmpty()) {
            return;
        }
        if (validNodeIds.size() > 1) {
            throw CatalogException.invalidArgument(
                    "单个业务对象只能绑定一个节点；如需批量绑定多个业务对象，请使用一对一批量绑定能力"
            );
        }
        bind(validNodeIds.get(0), bizId, bizType);
    }

    @Override
    public void batchBindByBizIds(List<Long> nodeIds, List<String> bizIds, String bizType) {
        if (nodeIds == null || bizIds == null || nodeIds.size() != bizIds.size()) {
            throw CatalogException.invalidArgument("nodeIds和bizIds长度必须相同");
        }
        if (nodeIds.isEmpty()) {
            return;
        }

        ensureLeafNodes(nodeIds);

        String normalizedBizType = trimToNull(bizType);
        if (normalizedBizType == null) {
            throw CatalogException.invalidArgument("bizType不能为空");
        }

        Map<String, Long> requestedBindings = new LinkedHashMap<>();
        for (int i = 0; i < nodeIds.size(); i++) {
            Long nodeId = nodeIds.get(i);
            String bizId = trimToNull(bizIds.get(i));
            if (nodeId == null || nodeId <= 0 || bizId == null) {
                continue;
            }
            Long previousNodeId = requestedBindings.putIfAbsent(bizId, nodeId);
            if (previousNodeId != null && !Objects.equals(previousNodeId, nodeId)) {
                throw CatalogException.invalidArgument("同一业务对象不能批量绑定到多个节点: " + bizId);
            }
        }

        if (requestedBindings.isEmpty()) {
            return;
        }

        Map<String, Long> existingBindings = resolveExistingBindingsByBizId(
                new ArrayList<>(requestedBindings.keySet()),
                normalizedBizType
        );

        List<CatalogRel> rels = new ArrayList<>(requestedBindings.size());
        for (Map.Entry<String, Long> entry : requestedBindings.entrySet()) {
            String bizId = entry.getKey();
            Long nodeId = entry.getValue();
            Long existingNodeId = existingBindings.get(bizId);
            if (existingNodeId != null) {
                if (Objects.equals(existingNodeId, nodeId)) {
                    continue;
                }
                throw CatalogException.bizAlreadyBound(bizId, normalizedBizType, existingNodeId);
            }

            CatalogRel rel = new CatalogRel();
            rel.setNodeId(nodeId);
            rel.setBizId(bizId);
            rel.setBizType(normalizedBizType);
            rels.add(rel);
        }

        if (!rels.isEmpty()) {
            relMapper.batchInsert(rels);
        }
    }

    @Override
    public void unbind(Long nodeId, String bizId, String bizType) {
        validateBindArgs(nodeId, bizId, bizType);
        relMapper.delete(nodeId, trimToNull(bizId), trimToNull(bizType));
    }

    /**
     * 返回完整目录的扁平节点列表，顺序与树前序遍历一致。
     */
    @Override
    public List<CatalogNode> listNodesInTreeOrder() {
        return sortNodesForTreeTraversal(nodeMapper.selectAll());
    }

    @Override
    public List<CatalogNode> getBizPath(String bizId, String bizType) {
        Long nodeId = resolveSingleBoundNodeId(trimToNull(bizId), trimToNull(bizType));
        if (nodeId == null) {
            return Collections.emptyList();
        }

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

    @Override
    public List<Long> getNodeIds(String bizId, String bizType) {
        Long nodeId = resolveSingleBoundNodeId(trimToNull(bizId), trimToNull(bizType));
        if (nodeId == null) {
            return Collections.emptyList();
        }
        return List.of(nodeId);
    }

    @Override
    public List<String> getBizIds(Long nodeId, String bizType) {
        return relMapper.selectByNode(nodeId, bizType)
                .stream()
                .map(CatalogRel::getBizId)
                .toList();
    }

    @Override
    public List<String> getBizIdsByNodeTree(Long nodeId, String bizType) {
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

    /**
     * 返回用于还原业务局部树的扁平节点列表，包含绑定节点及其祖先节点。
     */
    @Override
    public List<CatalogNode> listBizRelatedNodes(String bizId, String bizType) {
        Long boundNodeId = resolveSingleBoundNodeId(trimToNull(bizId), trimToNull(bizType));
        if (boundNodeId == null) {
            return Collections.emptyList();
        }

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

    /**
     * 返回指定节点子树的扁平节点列表，包含当前节点及全部后代节点。
     */
    @Override
    public List<CatalogNode> listSubtreeNodes(Long nodeId) {
        CatalogNode node = nodeMapper.selectById(nodeId);
        if (node == null) {
            return Collections.emptyList();
        }
        if (!StringUtils.hasText(node.getPath())) {
            return List.of(node);
        }
        return sortNodesForTreeTraversal(nodeMapper.selectByPathPrefix(node.getPath()));
    }

    private CatalogNode getRequiredNode(Long nodeId) {
        if (nodeId == null || nodeId <= 0) {
            throw CatalogException.invalidArgument("节点ID无效");
        }
        CatalogNode node = nodeMapper.selectById(nodeId);
        if (node == null) {
            throw CatalogException.nodeNotFound(nodeId);
        }
        return node;
    }

    private void validateMoveTarget(CatalogNode node, Long newParentId, CatalogNode newParent) {
        if (!StringUtils.hasText(node.getPath())) {
            throw CatalogException.invalidArgument("节点路径无效: " + node.getId());
        }

        if (Objects.equals(node.getId(), newParentId)) {
            throw CatalogException.cannotMoveToSelf(node.getId());
        }

        if (newParent != null && StringUtils.hasText(newParent.getPath())) {
            String parentPath = newParent.getPath();
            String nodePath = node.getPath();
            if (Objects.equals(parentPath, nodePath) || parentPath.startsWith(nodePath + "/")) {
                throw CatalogException.cannotMoveToSelf(node.getId());
            }
        }
    }

    private void reorderWithinSameParent(Long nodeId, Long parentId, int oldSort, int newSort) {
        if (newSort < oldSort) {
            nodeMapper.incrementSortRange(parentId, newSort, oldSort - 1, nodeId);
        } else {
            nodeMapper.decrementSortRange(parentId, oldSort + 1, newSort, nodeId);
        }
        nodeMapper.updateSort(nodeId, newSort);
    }

    private int resolveTargetSort(Long parentId, Integer targetIndex, Long currentParentId) {
        Integer maxSortValue = nodeMapper.selectMaxSortByParent(parentId);
        int maxSort = maxSortValue == null ? 0 : maxSortValue;

        if (targetIndex == null) {
            if (Objects.equals(parentId, currentParentId)) {
                return maxSort;
            }
            return maxSort + 1;
        }

        int desiredSort = Math.max(1, targetIndex + 1);

        int maxAllowed = Objects.equals(parentId, currentParentId)
                ? Math.max(maxSort, 1)
                : maxSort + 1;

        return Math.min(desiredSort, maxAllowed);
    }

    private int nextAppendSort(Long parentId) {
        Integer maxSort = nodeMapper.selectMaxSortByParent(parentId);
        return maxSort == null ? 1 : maxSort + 1;
    }

    private int countBindings(List<Long> nodeIds) {
        Integer count = relMapper.countByNodeIds(nodeIds);
        return count == null ? 0 : count;
    }

    private Long normalizeParentId(Long parentId) {
        if (parentId == null || parentId <= 0) {
            return ROOT_PARENT_ID;
        }
        return parentId;
    }

    private CatalogNode getParentNode(Long parentId) {
        if (Objects.equals(parentId, ROOT_PARENT_ID)) {
            return null;
        }
        CatalogNode parent = nodeMapper.selectById(parentId);
        if (parent == null) {
            throw CatalogException.parentNotFound(parentId);
        }
        return parent;
    }

    private String buildPath(CatalogNode parent, Long nodeId) {
        if (parent == null || !StringUtils.hasText(parent.getPath())) {
            return "/" + nodeId;
        }
        return parent.getPath() + "/" + nodeId;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private int defaultSort(Integer sort) {
        return sort == null || sort <= 0 ? 1 : sort;
    }

    private int defaultLevel(Integer level) {
        return level == null || level <= 0 ? 1 : level;
    }

    private void validateBindArgs(Long nodeId, String bizId, String bizType) {
        if (nodeId == null || nodeId <= 0) {
            throw CatalogException.invalidArgument("节点ID无效");
        }
        if (!StringUtils.hasText(bizId)) {
            throw CatalogException.invalidArgument("bizId不能为空");
        }
        if (!StringUtils.hasText(bizType)) {
            throw CatalogException.invalidArgument("bizType不能为空");
        }
    }

    private void ensureLeafNode(Long nodeId) {
        Integer childCount = nodeMapper.countChildren(nodeId);
        if (childCount != null && childCount > 0) {
            throw CatalogException.notLeafNode(nodeId);
        }
    }

    private void ensureLeafNodes(List<Long> nodeIds) {
        List<Long> validIds = nodeIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (validIds.isEmpty()) {
            throw CatalogException.invalidArgument("节点ID列表为空");
        }

        List<Long> nonLeafIds = nodeMapper.selectIdsHavingChildren(validIds);
        if (nonLeafIds != null && !nonLeafIds.isEmpty()) {
            throw CatalogException.notLeafNode(nonLeafIds.get(0));
        }
    }

    private Long resolveSingleBoundNodeId(String bizId, String bizType) {
        if (bizId == null || bizType == null) {
            return null;
        }

        List<Long> nodeIds = relMapper.selectByBiz(bizId, bizType).stream()
                .map(CatalogRel::getNodeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (nodeIds.isEmpty()) {
            return null;
        }
        if (nodeIds.size() > 1) {
            throw CatalogException.bizBoundToMultipleNodes(bizId, bizType, nodeIds);
        }
        return nodeIds.get(0);
    }

    private Map<String, Long> resolveExistingBindingsByBizId(List<String> bizIds, String bizType) {
        List<String> normalizedBizIds = bizIds.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedBizIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<Long>> nodeIdsByBizId = relMapper.selectByBizIds(normalizedBizIds, bizType).stream()
                .filter(item -> item.getBizId() != null)
                .collect(Collectors.groupingBy(
                        CatalogRel::getBizId,
                        LinkedHashMap::new,
                        Collectors.mapping(CatalogRel::getNodeId, Collectors.toList())
                ));

        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Long>> entry : nodeIdsByBizId.entrySet()) {
            List<Long> distinctNodeIds = entry.getValue().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (distinctNodeIds.isEmpty()) {
                continue;
            }
            if (distinctNodeIds.size() > 1) {
                throw CatalogException.bizBoundToMultipleNodes(entry.getKey(), bizType, distinctNodeIds);
            }
            result.put(entry.getKey(), distinctNodeIds.get(0));
        }
        return result;
    }

    private void normalizeSiblingSorts(Long parentId) {
        List<CatalogNode> siblings = nodeMapper.selectByParentId(parentId);
        int expectedSort = 1;

        for (CatalogNode sibling : siblings) {
            if (sibling == null) {
                continue;
            }
            if (sibling.getSort() == null || sibling.getSort() != expectedSort) {
                nodeMapper.updateSort(sibling.getId(), expectedSort);
            }
            expectedSort++;
        }
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
            // 子树查询或业务相关查询拿到的往往只是局部节点集合，祖先节点可能并不完整。
            // 这类节点会被当成当前结果集里的“本地根节点”，保证返回顺序仍然可遍历、可展示。
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
}
