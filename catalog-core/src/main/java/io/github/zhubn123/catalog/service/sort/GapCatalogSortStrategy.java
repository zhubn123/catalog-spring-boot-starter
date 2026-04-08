package io.github.zhubn123.catalog.service.sort;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.exception.CatalogException;
import io.github.zhubn123.catalog.service.CatalogSortStrategy;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 默认的间隙排序策略。
 *
 * <p>通过固定步长为同级节点分配 sort 值；插入中间位置时优先复用相邻节点之间的空隙，
 * 只有间隙耗尽时才需要重排同级节点。</p>
 */
public final class GapCatalogSortStrategy implements CatalogSortStrategy {

    public static final String NAME = "gap";
    public static final int DEFAULT_STEP = 1024;

    private final int step;

    public GapCatalogSortStrategy() {
        this(DEFAULT_STEP);
    }

    public GapCatalogSortStrategy(int step) {
        if (step <= 1) {
            throw new IllegalArgumentException("catalog.sort.gap-step must be greater than 1");
        }
        this.step = step;
    }

    public int getStep() {
        return step;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int normalizeSort(Integer sort) {
        return sort == null || sort <= 0 ? step : sort;
    }

    @Override
    public int nextAppendSort(Integer maxSort) {
        return maxSort == null ? step : addStep(normalizeSort(maxSort), 1);
    }

    @Override
    public Integer resolveTargetSort(List<CatalogNode> siblings, int targetIndex) {
        List<CatalogNode> orderedSiblings = orderSiblings(siblings);

        Integer previousSort = targetIndex <= 0
                ? null
                : normalizeSort(orderedSiblings.get(targetIndex - 1).getSort());
        Integer nextSort = targetIndex >= orderedSiblings.size()
                ? null
                : normalizeSort(orderedSiblings.get(targetIndex).getSort());

        if (previousSort == null && nextSort == null) {
            return step;
        }
        if (previousSort == null) {
            int candidate = nextSort / 2;
            return candidate > 0 ? candidate : null;
        }
        if (nextSort == null) {
            return addStep(previousSort, 1);
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

    @Override
    public Map<Long, Integer> rebalance(List<CatalogNode> siblings) {
        if (siblings == null || siblings.isEmpty()) {
            return Collections.emptyMap();
        }

        List<CatalogNode> orderedSiblings = orderSiblings(siblings);
        Map<Long, Integer> updates = new LinkedHashMap<>();
        int expectedSort = step;
        for (CatalogNode sibling : orderedSiblings) {
            if (sibling == null || sibling.getId() == null) {
                expectedSort = addStep(expectedSort, 1);
                continue;
            }
            if (!Objects.equals(sibling.getSort(), expectedSort)) {
                sibling.setSort(expectedSort);
                updates.put(sibling.getId(), expectedSort);
            }
            expectedSort = addStep(expectedSort, 1);
        }
        return updates;
    }

    private List<CatalogNode> orderSiblings(List<CatalogNode> siblings) {
        if (siblings == null || siblings.isEmpty()) {
            return Collections.emptyList();
        }
        List<CatalogNode> orderedSiblings = siblings.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        orderedSiblings.sort(Comparator
                .comparingInt((CatalogNode sibling) -> normalizeSort(sibling == null ? null : sibling.getSort()))
                .thenComparing(sibling -> sibling == null || sibling.getId() == null ? Long.MAX_VALUE : sibling.getId()));
        return orderedSiblings;
    }

    private int addStep(int baseSort, int stepCount) {
        try {
            return Math.addExact(baseSort, Math.multiplyExact(step, stepCount));
        } catch (ArithmeticException ex) {
            throw CatalogException.invalidArgument("同级排序值已超过上限，请先执行排序修复");
        }
    }
}
