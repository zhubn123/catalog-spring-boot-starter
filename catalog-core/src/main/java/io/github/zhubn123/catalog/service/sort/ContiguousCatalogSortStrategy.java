package io.github.zhubn123.catalog.service.sort;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.exception.CatalogException;
import io.github.zhubn123.catalog.service.CatalogSortStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 连续整数排序策略。
 *
 * <p>同级节点始终维持 {@code 1, 2, 3...} 的连续顺序，适合中小规模且更强调
 * 排序值可读性的场景；代价是插入中间位置或跨父节点迁移时，需要同步调整更多兄弟节点。</p>
 */
public final class ContiguousCatalogSortStrategy implements CatalogSortStrategy {

    public static final String NAME = "contiguous";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int normalizeSort(Integer sort) {
        return sort == null || sort <= 0 ? 1 : sort;
    }

    @Override
    public int nextAppendSort(Integer maxSort) {
        if (maxSort == null) {
            return 1;
        }
        return increment(normalizeSort(maxSort));
    }

    @Override
    public Integer resolveTargetSort(List<CatalogNode> siblings, int targetIndex) {
        if (targetIndex < 0) {
            return 1;
        }
        return increment(targetIndex);
    }

    @Override
    public Map<Long, Integer> resolveMoveSorts(List<CatalogNode> siblings, CatalogNode movingNode, int targetIndex) {
        if (movingNode == null || movingNode.getId() == null) {
            return null;
        }

        List<CatalogNode> orderedSiblings = orderSiblings(siblings);
        int normalizedIndex = Math.max(0, Math.min(targetIndex, orderedSiblings.size()));
        orderedSiblings.add(normalizedIndex, movingNode);

        Map<Long, Integer> updates = new LinkedHashMap<>();
        int expectedSort = 1;
        for (CatalogNode sibling : orderedSiblings) {
            if (sibling == null || sibling.getId() == null) {
                expectedSort = increment(expectedSort);
                continue;
            }
            if (Objects.equals(sibling.getId(), movingNode.getId()) || !Objects.equals(sibling.getSort(), expectedSort)) {
                sibling.setSort(expectedSort);
                updates.put(sibling.getId(), expectedSort);
            }
            expectedSort = increment(expectedSort);
        }
        return updates;
    }

    @Override
    public Map<Long, Integer> rebalance(List<CatalogNode> siblings) {
        if (siblings == null || siblings.isEmpty()) {
            return Collections.emptyMap();
        }

        List<CatalogNode> orderedSiblings = orderSiblings(siblings);
        Map<Long, Integer> updates = new LinkedHashMap<>();
        int expectedSort = 1;
        for (CatalogNode sibling : orderedSiblings) {
            if (sibling == null || sibling.getId() == null) {
                expectedSort = increment(expectedSort);
                continue;
            }
            if (!Objects.equals(sibling.getSort(), expectedSort)) {
                sibling.setSort(expectedSort);
                updates.put(sibling.getId(), expectedSort);
            }
            expectedSort = increment(expectedSort);
        }
        return updates;
    }

    @Override
    public boolean requiresSiblingCompactionAfterRemoval() {
        return true;
    }

    private List<CatalogNode> orderSiblings(List<CatalogNode> siblings) {
        if (siblings == null || siblings.isEmpty()) {
            return new ArrayList<>();
        }
        List<CatalogNode> orderedSiblings = new ArrayList<>();
        for (CatalogNode sibling : siblings) {
            if (sibling != null) {
                orderedSiblings.add(sibling);
            }
        }
        orderedSiblings.sort(Comparator
                .comparingInt((CatalogNode sibling) -> normalizeSort(sibling.getSort()))
                .thenComparing(sibling -> sibling.getId() == null ? Long.MAX_VALUE : sibling.getId()));
        return orderedSiblings;
    }

    private int increment(int value) {
        try {
            return Math.addExact(value, 1);
        } catch (ArithmeticException ex) {
            throw CatalogException.invalidArgument("同级排序值已超过上限，请调整排序字段类型或拆分热点父节点");
        }
    }
}
