package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.service.sort.ContiguousCatalogSortStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContiguousCatalogSortStrategyTest {

    @Test
    void nextAppendSortUsesDenseSequence() {
        ContiguousCatalogSortStrategy strategy = new ContiguousCatalogSortStrategy();

        assertThat(strategy.nextAppendSort(null)).isEqualTo(1);
        assertThat(strategy.nextAppendSort(3)).isEqualTo(4);
    }

    @Test
    void resolveMoveSortsReordersWholeSiblingRange() {
        ContiguousCatalogSortStrategy strategy = new ContiguousCatalogSortStrategy();
        CatalogNode movingNode = node(3L, 3);

        Map<Long, Integer> updates = strategy.resolveMoveSorts(List.of(
                node(1L, 1),
                node(2L, 2)
        ), movingNode, 0);

        assertThat(updates).containsExactly(
                Map.entry(3L, 1),
                Map.entry(1L, 2),
                Map.entry(2L, 3)
        );
    }

    @Test
    void rebalanceCompactsSortValuesToContiguousSequence() {
        ContiguousCatalogSortStrategy strategy = new ContiguousCatalogSortStrategy();

        CatalogNode first = node(10L, 10);
        CatalogNode second = node(20L, 20);
        CatalogNode third = node(30L, 30);

        Map<Long, Integer> updates = strategy.rebalance(List.of(first, second, third));

        assertThat(updates).containsExactly(
                Map.entry(10L, 1),
                Map.entry(20L, 2),
                Map.entry(30L, 3)
        );
    }

    @Test
    void requiresSiblingCompactionAfterRemoval() {
        ContiguousCatalogSortStrategy strategy = new ContiguousCatalogSortStrategy();

        assertThat(strategy.requiresSiblingCompactionAfterRemoval()).isTrue();
    }

    private CatalogNode node(Long id, Integer sort) {
        CatalogNode node = new CatalogNode();
        node.setId(id);
        node.setSort(sort);
        return node;
    }
}
