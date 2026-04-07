package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogTreeNode;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogTreeAssemblerTest {

    @Test
    void assembleMarksLeafAndAppliesEnricher() {
        CatalogTreeAssembler assembler = new CatalogTreeAssembler(List.of((treeNode, sourceNode, context) -> {
            if (Boolean.TRUE.equals(treeNode.getLeaf())) {
                treeNode.getExtensions().put("bizBindingPlaceholder", "reserved");
            }
            treeNode.getExtensions().put("scene", context.scene().name());
        }));

        List<CatalogTreeNode> tree = assembler.assemble(List.of(
                node(1L, 0L, "Root", 1),
                node(2L, 1L, "Leaf", 1)
        ), CatalogTreeAssembleContext.bizTree("biz-1", "deliver"));

        assertThat(tree).singleElement().satisfies(root -> {
            assertThat(root.getLeaf()).isFalse();
            assertThat(root.getBindable()).isFalse();
            assertThat(root.getExtensions()).containsEntry("scene", "BIZ_TREE");
            assertThat(root.getChildren()).singleElement().satisfies(leaf -> {
                assertThat(leaf.getLeaf()).isTrue();
                assertThat(leaf.getBindable()).isTrue();
                assertThat(leaf.getExtensions())
                        .containsEntry("scene", "BIZ_TREE")
                        .containsEntry("bizBindingPlaceholder", "reserved");
            });
        });
    }

    @Test
    void assembleCanReusePreparedStateAcrossNodes() {
        AtomicInteger prepareCalls = new AtomicInteger();
        CatalogTreeAssembler assembler = new CatalogTreeAssembler(List.of(new CatalogTreeNodeEnricher() {
            @Override
            public void enrich(CatalogTreeNode treeNode, CatalogNode sourceNode, CatalogTreeAssembleContext context) {
                throw new AssertionError("prepared enrich overload should be used");
            }

            @Override
            public Object prepare(List<CatalogNode> sourceNodes, CatalogTreeAssembleContext context) {
                prepareCalls.incrementAndGet();
                Map<Long, String> bizIdsByNodeId = new LinkedHashMap<>();
                bizIdsByNodeId.put(2L, "DELIVER-001");
                return bizIdsByNodeId;
            }

            @Override
            public void enrich(
                    CatalogTreeNode treeNode,
                    CatalogNode sourceNode,
                    CatalogTreeAssembleContext context,
                    Object preparedState
            ) {
                @SuppressWarnings("unchecked")
                Map<Long, String> bizIdsByNodeId = (Map<Long, String>) preparedState;
                if (bizIdsByNodeId != null && bizIdsByNodeId.containsKey(sourceNode.getId())) {
                    treeNode.getExtensions().put("bizId", bizIdsByNodeId.get(sourceNode.getId()));
                }
            }
        }));

        List<CatalogTreeNode> tree = assembler.assemble(List.of(
                node(1L, 0L, "Root", 1),
                node(2L, 1L, "Leaf", 1)
        ), CatalogTreeAssembleContext.fullTree());

        assertThat(prepareCalls).hasValue(1);
        assertThat(tree).singleElement().satisfies(root -> {
            assertThat(root.getExtensions()).doesNotContainKey("bizId");
            assertThat(root.getChildren()).singleElement().satisfies(leaf ->
                    assertThat(leaf.getExtensions()).containsEntry("bizId", "DELIVER-001"));
        });
    }

    private CatalogNode node(Long id, Long parentId, String name, Integer sort) {
        CatalogNode node = new CatalogNode();
        node.setId(id);
        node.setParentId(parentId);
        node.setName(name);
        node.setSort(sort);
        return node;
    }
}
