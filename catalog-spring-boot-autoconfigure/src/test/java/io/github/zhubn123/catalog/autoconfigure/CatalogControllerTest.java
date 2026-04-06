package io.github.zhubn123.catalog.autoconfigure;

import io.github.zhubn123.catalog.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    @Mock
    private CatalogService catalogService;

    private CatalogController controller;

    @BeforeEach
    void setUp() {
        controller = new CatalogController();
        ReflectionTestUtils.setField(controller, "catalogService", catalogService);
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
    void nodesUsesStructuredListEndpoint() {
        controller.nodes();

        verify(catalogService).listNodesInTreeOrder();
    }

    @Test
    void treeKeepsLegacyCompatibilityAlias() {
        controller.tree();

        verify(catalogService).listNodesInTreeOrder();
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
}
