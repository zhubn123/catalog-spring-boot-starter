package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogTreeNode;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    private CatalogNode node(Long id, Long parentId, String name, Integer sort) {
        CatalogNode node = new CatalogNode();
        node.setId(id);
        node.setParentId(parentId);
        node.setName(name);
        node.setSort(sort);
        return node;
    }
}
