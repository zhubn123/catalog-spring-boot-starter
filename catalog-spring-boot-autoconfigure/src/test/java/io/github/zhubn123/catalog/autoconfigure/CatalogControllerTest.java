package io.github.zhubn123.catalog.autoconfigure;

import io.github.zhubn123.catalog.domain.CatalogPage;
import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogSortRepairResult;
import io.github.zhubn123.catalog.domain.CatalogTreeNode;
import io.github.zhubn123.catalog.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    @Mock
    private CatalogService catalogService;

    private CatalogController controller;

    @BeforeEach
    void setUp() {
        controller = new CatalogController(catalogService);
    }

    @Test
    void batchAddNodeAcceptsStructuredRequestBody() {
        controller.batchAddNode(
                new CatalogController.BatchAddNodeRequest(1L, List.of(" 合同 ", "", "交付物")),
                null,
                null
        );

        ArgumentCaptor<String[]> namesCaptor = ArgumentCaptor.forClass(String[].class);
        verify(catalogService).batchAddNode(eq(1L), namesCaptor.capture());
        assertThat(namesCaptor.getValue()).containsExactly("合同", "交付物");
    }

    @Test
    void batchBindPairsAcceptsStructuredRequestBody() {
        controller.batchBindPairs(
                new CatalogController.BatchBindPairsRequest(List.of(11L, 12L), List.of(" D-1 ", "D-2"), "deliver"),
                null,
                null,
                null
        );

        verify(catalogService).batchBindByBizIds(List.of(11L, 12L), List.of("D-1", "D-2"), "deliver");
    }

    @Test
    void batchBindPairsKeepsLegacyCsvCompatibility() {
        controller.batchBindPairs(null, "11, 12", " D-1 , D-2 ", "deliver");

        verify(catalogService).batchBindByBizIds(List.of(11L, 12L), List.of("D-1", "D-2"), "deliver");
    }

    @Test
    void repairSortUsesExplicitMaintenanceEndpoint() {
        when(catalogService.repairSiblingSorts(0L))
                .thenReturn(new CatalogSortRepairResult("PARENT", 0L, 1, 3, 2));

        CatalogSortRepairResult result = controller.repairSort(0L);

        verify(catalogService).repairSiblingSorts(0L);
        assertThat(result.getUpdatedNodes()).isEqualTo(2);
    }

    @Test
    void repairAllSortsUsesGlobalMaintenanceEndpoint() {
        when(catalogService.repairAllSiblingSorts())
                .thenReturn(new CatalogSortRepairResult("ALL", null, 4, 15, 5));

        CatalogSortRepairResult result = controller.repairAllSorts();

        verify(catalogService).repairAllSiblingSorts();
        assertThat(result.getGroups()).isEqualTo(4);
    }

    @Test
    void childrenUsesDirectChildrenEndpoint() {
        CatalogNode child = new CatalogNode();
        child.setId(2L);
        when(catalogService.listChildrenNodes(9L)).thenReturn(List.of(child));

        List<CatalogNode> children = controller.children(9L);

        verify(catalogService).listChildrenNodes(9L);
        assertThat(children).extracting(CatalogNode::getId).containsExactly(2L);
    }

    @Test
    void childrenRejectsRootQuery() {
        assertThatThrownBy(() -> controller.children(0L))
                .hasMessageContaining("parentId");

        verifyNoInteractions(catalogService);
    }

    @Test
    void childrenPageUsesPagedChildrenEndpoint() {
        CatalogNode child = new CatalogNode();
        child.setId(3L);
        when(catalogService.pageChildrenNodes(0L, 2, 20))
                .thenReturn(new CatalogPage<>(2, 20, 21, false, List.of(child)));

        CatalogPage<CatalogNode> page = controller.childrenPage(0L, 2, 20);

        verify(catalogService).pageChildrenNodes(0L, 2, 20);
        assertThat(page.getPage()).isEqualTo(2);
        assertThat(page.getItems()).extracting(CatalogNode::getId).containsExactly(3L);
    }

    @Test
    void bizTreeNodesUsesStructuredListEndpoint() {
        controller.bizTreeNodes("biz-1", "deliver");

        verify(catalogService).listBizRelatedNodes("biz-1", "deliver");
    }

    @Test
    void subtreeNodesUsesStructuredListEndpoint() {
        controller.subtreeNodes(9L);

        verify(catalogService).listSubtreeNodes(9L);
    }

    @Test
    void bizTreeReturnsNestedTreeStructure() {
        CatalogTreeNode root = new CatalogTreeNode();
        root.setId(1L);
        when(catalogService.listBizRelatedTree("biz-1", "deliver")).thenReturn(List.of(root));

        List<CatalogTreeNode> tree = controller.bizTree("biz-1", "deliver");

        verify(catalogService).listBizRelatedTree("biz-1", "deliver");
        assertThat(tree).extracting(CatalogTreeNode::getId).containsExactly(1L);
    }

    @Test
    void nodeBindingsUsesDirectBindingEndpoint() {
        controller.nodeBindings(9L, "deliver");

        verify(catalogService).getBizIds(9L, "deliver");
    }

    @Test
    void subtreeReturnsNestedTreeStructure() {
        CatalogTreeNode root = new CatalogTreeNode();
        root.setId(9L);
        when(catalogService.listSubtreeTree(9L)).thenReturn(List.of(root));

        List<CatalogTreeNode> tree = controller.subtree(9L);

        verify(catalogService).listSubtreeTree(9L);
        assertThat(tree).extracting(CatalogTreeNode::getId).containsExactly(9L);
    }

    @Test
    void batchAddNodeRejectsBlankNames() {
        assertThatThrownBy(() -> controller.batchAddNode(
                new CatalogController.BatchAddNodeRequest(1L, List.of(" ", "")),
                null,
                null
        ))
                .hasMessageContaining("names");

        verifyNoInteractions(catalogService);
    }

    @Test
    void batchBindPairsRejectsMismatchedPairCount() {
        assertThatThrownBy(() -> controller.batchBindPairs(
                new CatalogController.BatchBindPairsRequest(List.of(11L, 12L), List.of("D-1"), "deliver"),
                null,
                null,
                null
        ))
                .hasMessageContaining("长度必须一致");

        verifyNoInteractions(catalogService);
    }

    @Test
    void moveRejectsNegativeTargetIndex() {
        assertThatThrownBy(() -> controller.move(
                new CatalogController.MoveNodeRequest(9L, 1L, -1),
                null,
                null,
                null
        ))
                .hasMessageContaining("targetIndex");

        verifyNoInteractions(catalogService);
    }

    @Test
    void updateNodeRejectsEmptyMutationRequest() {
        assertThatThrownBy(() -> controller.updateNode(
                new CatalogController.UpdateNodeRequest(9L, " ", " ", null),
                null,
                null,
                null,
                null
        ))
                .hasMessageContaining("至少提供一个可更新字段");

        verifyNoInteractions(catalogService);
    }

    @Test
    void bizPathRejectsBlankBizType() {
        assertThatThrownBy(() -> controller.bizPath("biz-1", " "))
                .hasMessageContaining("bizType");

        verifyNoInteractions(catalogService);
    }
}
