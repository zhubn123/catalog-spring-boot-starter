package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GapCatalogSortStrategyTest {

    @Test
    void nextAppendSortUsesConfiguredStep() {
        GapCatalogSortStrategy strategy = new GapCatalogSortStrategy(128);

        assertThat(strategy.nextAppendSort(null)).isEqualTo(128);
        assertThat(strategy.nextAppendSort(256)).isEqualTo(384);
    }

    @Test
    void resolveTargetSortReturnsMidpointWhenGapExists() {
        GapCatalogSortStrategy strategy = new GapCatalogSortStrategy();

        Integer targetSort = strategy.resolveTargetSort(List.of(
                node(1L, 1024),
                node(2L, 2048)
        ), 1);

        assertThat(targetSort).isEqualTo(1536);
    }

    @Test
    void resolveTargetSortReturnsNullWhenGapIsExhausted() {
        GapCatalogSortStrategy strategy = new GapCatalogSortStrategy();

        Integer targetSort = strategy.resolveTargetSort(List.of(
                node(1L, 1024),
                node(2L, 1025)
        ), 1);

        assertThat(targetSort).isNull();
    }

    @Test
    void rebalanceRebuildsSiblingOrderingWithConfiguredStep() {
        GapCatalogSortStrategy strategy = new GapCatalogSortStrategy(128);

        CatalogNode first = node(1L, 4096);
        CatalogNode second = node(2L, 128);
        CatalogNode third = node(3L, 1024);

        Map<Long, Integer> updates = strategy.rebalance(List.of(first, second, third));

        assertThat(updates).containsExactly(
                Map.entry(3L, 256),
                Map.entry(1L, 384)
        );
        assertThat(List.of(first, second, third))
                .extracting(CatalogNode::getSort)
                .containsExactly(384, 128, 256);
    }

    @Test
    void constructorRejectsInvalidGapStep() {
        assertThatThrownBy(() -> new GapCatalogSortStrategy(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than 1");
    }

    private CatalogNode node(Long id, Integer sort) {
        CatalogNode node = new CatalogNode();
        node.setId(id);
        node.setSort(sort);
        return node;
    }
}
