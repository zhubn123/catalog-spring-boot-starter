package io.github.zhubn123.catalog.service.command;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogSortRepairResult;
import io.github.zhubn123.catalog.exception.CatalogException;
import io.github.zhubn123.catalog.mapper.CatalogNodeMapper;
import io.github.zhubn123.catalog.mapper.CatalogRelMapper;
import io.github.zhubn123.catalog.service.CatalogSortStrategy;
import io.github.zhubn123.catalog.service.sort.GapCatalogSortStrategy;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 负责目录节点命令侧逻辑。
 *
 * <p>包含新增、移动、更新、删除以及同级排序维护等写操作。</p>
 *
 * <p>写路径默认依赖可插拔的 {@link CatalogSortStrategy} 计算 sort 值，
 * 当前内置 gap 排序，同时为后续扩展连续排序等整数型策略保留统一注入点。</p>
 */
public final class CatalogNodeCommandService {

    private static final Long ROOT_PARENT_ID = 0L;

    private final CatalogNodeMapper nodeMapper;
    private final CatalogRelMapper relMapper;
    private final CatalogSortStrategy sortStrategy;

    public CatalogNodeCommandService(CatalogNodeMapper nodeMapper, CatalogRelMapper relMapper) {
        this(nodeMapper, relMapper, new GapCatalogSortStrategy());
    }

    public CatalogNodeCommandService(
            CatalogNodeMapper nodeMapper,
            CatalogRelMapper relMapper,
            CatalogSortStrategy sortStrategy
    ) {
        this.nodeMapper = nodeMapper;
        this.relMapper = relMapper;
        this.sortStrategy = sortStrategy == null ? new GapCatalogSortStrategy() : sortStrategy;
    }

    public Long addNode(Long parentId, String name) {
        Long normalizedParentId = normalizeParentId(parentId);
        String nodeName = trimToNull(name);
        if (nodeName == null) {
            throw CatalogException.nameBlank();
        }

        CatalogNode parent = getParentNode(normalizedParentId);
        CatalogNode node = new CatalogNode();
        node.setParentId(normalizedParentId);
        node.setName(nodeName);
        node.setLevel(parent == null ? 1 : parent.getLevel() + 1);
        node.setSort(nextAppendSort(normalizedParentId));

        nodeMapper.insert(node);
        nodeMapper.updatePath(node.getId(), buildPath(parent, node.getId()));
        return node.getId();
    }

    public List<Long> batchAddNode(Long parentId, String[] names) {
        if (names == null || names.length == 0) {
            return Collections.emptyList();
        }

        Long normalizedParentId = normalizeParentId(parentId);
        CatalogNode parent = getParentNode(normalizedParentId);
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
            // 后续批量节点直接基于前一个待插入节点继续推导，避免为每个节点重新查一次数据库最大 sort。
            int sort = i == 0 ? startSort : sortStrategy.nextAppendSort(nodes.get(i - 1).getSort());
            node.setSort(sort);
            nodes.add(node);
        }

        nodeMapper.batchInsert(nodes);
        for (CatalogNode node : nodes) {
            node.setPath(buildPath(parent, node.getId()));
        }
        nodeMapper.batchUpdatePath(nodes);

        return nodes.stream().map(CatalogNode::getId).toList();
    }

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

    public void moveNode(Long nodeId, Long parentId) {
        moveNode(nodeId, parentId, null);
    }

    public void moveNode(Long nodeId, Long parentId, Integer targetIndex) {
        CatalogNode node = getRequiredNode(nodeId);

        Long oldParentId = normalizeParentId(node.getParentId());
        Long newParentId = normalizeParentId(parentId);

        CatalogNode newParent = getParentNode(newParentId);
        validateMoveTarget(node, newParentId, newParent);

        if (Objects.equals(oldParentId, newParentId)) {
            applySameParentMove(node, newParentId, targetIndex);
            return;
        }

        Map<Long, Integer> targetSortUpdates = resolveMoveSorts(node, newParentId, targetIndex);
        int newSort = requireTargetSort(node, targetSortUpdates);
        applySortUpdates(targetSortUpdates, nodeId);

        String newPath = buildPath(newParent, nodeId);
        int oldLevel = node.getLevel() == null ? 1 : node.getLevel();
        int parentLevel = newParent == null ? 0 : defaultLevel(newParent.getLevel());
        int newLevel = parentLevel + 1;
        int levelDelta = newLevel - oldLevel;

        nodeMapper.updateParentLevelPathSort(nodeId, newParentId, newLevel, newPath, newSort);
        nodeMapper.moveSubtree(node.getPath(), newPath, levelDelta);
        compactSiblingsAfterRemoval(oldParentId);
    }

    public void deleteNode(Long nodeId, boolean recursive) {
        CatalogNode node = getRequiredNode(nodeId);

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
        compactSiblingsAfterRemoval(node.getParentId());
    }

    public CatalogSortRepairResult repairSiblingSorts(Long parentId) {
        Long normalizedParentId = normalizeParentId(parentId);
        List<CatalogNode> siblings = new ArrayList<>(nodeMapper.selectByParentId(normalizedParentId));
        int updatedNodes = rebalanceSiblingSorts(siblings);
        return new CatalogSortRepairResult("PARENT", normalizedParentId, 1, siblings.size(), updatedNodes);
    }

    public CatalogSortRepairResult repairAllSiblingSorts() {
        List<CatalogNode> nodes = nodeMapper.selectAll();
        if (nodes == null || nodes.isEmpty()) {
            return new CatalogSortRepairResult("ALL", null, 0, 0, 0);
        }

        Map<Long, List<CatalogNode>> siblingsByParent = new LinkedHashMap<>();
        for (CatalogNode node : nodes) {
            siblingsByParent.computeIfAbsent(normalizeParentId(node.getParentId()), key -> new ArrayList<>()).add(node);
        }

        int scannedNodes = 0;
        int updatedNodes = 0;
        for (List<CatalogNode> siblings : siblingsByParent.values()) {
            scannedNodes += siblings.size();
            updatedNodes += rebalanceSiblingSorts(siblings);
        }
        return new CatalogSortRepairResult("ALL", null, siblingsByParent.size(), scannedNodes, updatedNodes);
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

    private void applySameParentMove(CatalogNode node, Long parentId, Integer targetIndex) {
        Map<Long, Integer> sortUpdates = resolveMoveSorts(node, parentId, targetIndex);
        int currentSort = sortStrategy.normalizeSort(node.getSort());
        Integer targetSort = sortUpdates.get(node.getId());
        if (sortUpdates.size() == 1 && targetSort != null && targetSort == currentSort) {
            return;
        }
        applySortUpdates(sortUpdates, null);
    }

    private Map<Long, Integer> resolveMoveSorts(CatalogNode node, Long parentId, Integer targetIndex) {
        Long currentParentId = normalizeParentId(node.getParentId());
        int currentSort = sortStrategy.normalizeSort(node.getSort());

        if (targetIndex == null && !sortStrategy.requiresSiblingCompactionAfterRemoval()) {
            // gap 策略下“追加到末尾”可以走快速路径，只关心当前最大 sort，不需要整组兄弟节点参与计算。
            Integer maxSortValue = nodeMapper.selectMaxSortByParent(parentId);
            if (maxSortValue == null) {
                return singleNodeSort(node.getId(), sortStrategy.nextAppendSort(null));
            }
            int normalizedMaxSort = sortStrategy.normalizeSort(maxSortValue);
            if (Objects.equals(parentId, currentParentId) && currentSort >= normalizedMaxSort) {
                return singleNodeSort(node.getId(), currentSort);
            }
            return singleNodeSort(node.getId(), sortStrategy.nextAppendSort(maxSortValue));
        }

        List<CatalogNode> siblings = new ArrayList<>(nodeMapper.selectByParentId(parentId));
        siblings.removeIf(sibling -> Objects.equals(sibling.getId(), node.getId()));

        int normalizedIndex = targetIndex == null
                ? siblings.size()
                : Math.max(0, Math.min(targetIndex, siblings.size()));
        Map<Long, Integer> resolvedSorts = sortStrategy.resolveMoveSorts(siblings, node, normalizedIndex);
        if (resolvedSorts != null) {
            return resolvedSorts;
        }

        // 只有当前局部空隙不够时才做一次显式重排，尽量把重写范围限制在单个父节点下。
        rebalanceSiblingSorts(siblings);
        resolvedSorts = sortStrategy.resolveMoveSorts(siblings, node, normalizedIndex);
        if (resolvedSorts != null) {
            return resolvedSorts;
        }

        throw CatalogException.invalidArgument("无法为目标位置分配排序值，请先修复当前父节点下的排序数据");
    }

    private int requireTargetSort(CatalogNode node, Map<Long, Integer> sortUpdates) {
        Integer targetSort = sortUpdates.get(node.getId());
        if (targetSort == null) {
            throw CatalogException.invalidArgument("无法计算目标排序值: " + node.getId());
        }
        return targetSort;
    }

    private int rebalanceSiblingSorts(List<CatalogNode> siblings) {
        Map<Long, Integer> updates = sortStrategy.rebalance(siblings);
        applySortUpdates(updates, null);
        return updates.size();
    }

    private void compactSiblingsAfterRemoval(Long parentId) {
        if (!sortStrategy.requiresSiblingCompactionAfterRemoval()) {
            return;
        }
        // 仅连续排序等“不能接受空洞”的策略才需要在删除/迁出后压紧原兄弟组。
        List<CatalogNode> siblings = new ArrayList<>(nodeMapper.selectByParentId(normalizeParentId(parentId)));
        rebalanceSiblingSorts(siblings);
    }

    private void applySortUpdates(Map<Long, Integer> sortUpdates, Long excludedNodeId) {
        if (sortUpdates == null || sortUpdates.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, Integer> entry : sortUpdates.entrySet()) {
            if (entry.getKey() == null || Objects.equals(entry.getKey(), excludedNodeId) || entry.getValue() == null) {
                continue;
            }
            nodeMapper.updateSort(entry.getKey(), entry.getValue());
        }
    }

    private Map<Long, Integer> singleNodeSort(Long nodeId, int sort) {
        Map<Long, Integer> sortUpdates = new LinkedHashMap<>();
        sortUpdates.put(nodeId, sort);
        return sortUpdates;
    }

    private int nextAppendSort(Long parentId) {
        return sortStrategy.nextAppendSort(nodeMapper.selectMaxSortByParent(parentId));
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

    private int defaultLevel(Integer level) {
        return level == null || level <= 0 ? 1 : level;
    }
}
