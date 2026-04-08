package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.exception.CatalogException;
import io.github.zhubn123.catalog.mapper.CatalogNodeMapper;
import io.github.zhubn123.catalog.mapper.CatalogRelMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 负责目录节点命令侧逻辑。
 *
 * <p>包含新增、移动、更新、删除以及同级排序维护等写操作。</p>
 *
 * <p>默认假设目录节点的排序数据由当前服务持续维护为一致状态，
 * 因此写入热路径不再主动扫描整组兄弟节点做排序归一化。</p>
 *
 * <p>当前默认采用“跳跃排序”策略：追加节点时按固定步长分配排序值，
 * 调整位置时优先复用相邻节点之间的空隙，只有间隔耗尽时才对局部兄弟节点做一次重排。
 * 如需修复历史脏数据，应通过单独的治理入口处理，而不是让每次写操作都承担全量扫描成本。</p>
 */
final class CatalogNodeCommandService {

    private static final Long ROOT_PARENT_ID = 0L;
    private static final int SORT_STEP = 1024;

    private final CatalogNodeMapper nodeMapper;
    private final CatalogRelMapper relMapper;

    CatalogNodeCommandService(CatalogNodeMapper nodeMapper, CatalogRelMapper relMapper) {
        this.nodeMapper = nodeMapper;
        this.relMapper = relMapper;
    }

    Long addNode(Long parentId, String name) {
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

    List<Long> batchAddNode(Long parentId, String[] names) {
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
            node.setSort(addSortStep(startSort, i));
            nodes.add(node);
        }

        nodeMapper.batchInsert(nodes);
        for (CatalogNode node : nodes) {
            node.setPath(buildPath(parent, node.getId()));
        }
        nodeMapper.batchUpdatePath(nodes);

        return nodes.stream().map(CatalogNode::getId).toList();
    }

    void updateNode(Long nodeId, String name, String code, Integer sort) {
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

    void moveNode(Long nodeId, Long parentId) {
        moveNode(nodeId, parentId, null);
    }

    void moveNode(Long nodeId, Long parentId, Integer targetIndex) {
        CatalogNode node = getRequiredNode(nodeId);

        Long oldParentId = normalizeParentId(node.getParentId());
        Long newParentId = normalizeParentId(parentId);

        CatalogNode newParent = getParentNode(newParentId);
        validateMoveTarget(node, newParentId, newParent);

        int oldSort = defaultSort(node.getSort());
        int newSort = resolveTargetSort(node, newParentId, targetIndex);

        if (Objects.equals(oldParentId, newParentId)) {
            if (newSort == oldSort) {
                return;
            }
            nodeMapper.updateSort(nodeId, newSort);
            return;
        }

        String newPath = buildPath(newParent, nodeId);
        int oldLevel = node.getLevel() == null ? 1 : node.getLevel();
        int parentLevel = newParent == null ? 0 : defaultLevel(newParent.getLevel());
        int newLevel = parentLevel + 1;
        int levelDelta = newLevel - oldLevel;

        nodeMapper.updateParentLevelPathSort(nodeId, newParentId, newLevel, newPath, newSort);
        nodeMapper.moveSubtree(node.getPath(), newPath, levelDelta);
    }

    void deleteNode(Long nodeId, boolean recursive) {
        CatalogNode node = getRequiredNode(nodeId);
        Long parentId = normalizeParentId(node.getParentId());

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

    private int resolveTargetSort(CatalogNode node, Long parentId, Integer targetIndex) {
        Long currentParentId = normalizeParentId(node.getParentId());
        int currentSort = defaultSort(node.getSort());

        if (targetIndex == null) {
            Integer maxSortValue = nodeMapper.selectMaxSortByParent(parentId);
            if (maxSortValue == null) {
                return SORT_STEP;
            }
            if (Objects.equals(parentId, currentParentId) && currentSort >= maxSortValue) {
                return currentSort;
            }
            return addSortStep(maxSortValue, 1);
        }

        List<CatalogNode> siblings = new ArrayList<>(nodeMapper.selectByParentId(parentId));
        siblings.removeIf(sibling -> Objects.equals(sibling.getId(), node.getId()));

        int normalizedIndex = Math.max(0, Math.min(targetIndex, siblings.size()));
        Integer resolvedSort = resolveSortBetweenSiblings(siblings, normalizedIndex);
        if (resolvedSort != null) {
            return resolvedSort;
        }

        rebalanceSiblingSorts(siblings);
        resolvedSort = resolveSortBetweenSiblings(siblings, normalizedIndex);
        if (resolvedSort != null) {
            return resolvedSort;
        }

        throw CatalogException.invalidArgument("无法为目标位置分配排序值，请先修复当前父节点下的排序数据");
    }

    private Integer resolveSortBetweenSiblings(List<CatalogNode> siblings, int targetIndex) {
        Integer previousSort = targetIndex <= 0
                ? null
                : defaultSort(siblings.get(targetIndex - 1).getSort());
        Integer nextSort = targetIndex >= siblings.size()
                ? null
                : defaultSort(siblings.get(targetIndex).getSort());

        if (previousSort == null && nextSort == null) {
            return SORT_STEP;
        }
        if (previousSort == null) {
            int candidate = nextSort / 2;
            return candidate > 0 ? candidate : null;
        }
        if (nextSort == null) {
            return addSortStep(previousSort, 1);
        }

        int gap = nextSort - previousSort;
        if (gap <= 1) {
            return null;
        }

        int candidate = previousSort + gap / 2;
        if (candidate == previousSort || candidate == nextSort) {
            return null;
        }
        return candidate;
    }

    private void rebalanceSiblingSorts(List<CatalogNode> siblings) {
        int expectedSort = SORT_STEP;
        for (CatalogNode sibling : siblings) {
            if (sibling == null || sibling.getId() == null) {
                expectedSort = addSortStep(expectedSort, 1);
                continue;
            }
            if (!Objects.equals(sibling.getSort(), expectedSort)) {
                nodeMapper.updateSort(sibling.getId(), expectedSort);
                sibling.setSort(expectedSort);
            }
            expectedSort = addSortStep(expectedSort, 1);
        }
    }

    private int nextAppendSort(Long parentId) {
        Integer maxSort = nodeMapper.selectMaxSortByParent(parentId);
        return maxSort == null ? SORT_STEP : addSortStep(maxSort, 1);
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
        return sort == null || sort <= 0 ? SORT_STEP : sort;
    }

    private int defaultLevel(Integer level) {
        return level == null || level <= 0 ? 1 : level;
    }

    private int addSortStep(int baseSort, int stepCount) {
        try {
            return Math.addExact(baseSort, Math.multiplyExact(SORT_STEP, stepCount));
        } catch (ArithmeticException ex) {
            throw CatalogException.invalidArgument("同级排序值已超过上限，请先执行排序修复");
        }
    }
}
