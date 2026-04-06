package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogRel;
import io.github.zhubn123.catalog.exception.CatalogException;
import io.github.zhubn123.catalog.mapper.CatalogNodeMapper;
import io.github.zhubn123.catalog.mapper.CatalogRelMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceImplTest {

    @Mock
    private CatalogNodeMapper nodeMapper;

    @Mock
    private CatalogRelMapper relMapper;

    @InjectMocks
    private CatalogServiceImpl service;

    @Test
    void addNodeCreatesRootNodeAndUpdatesPath() {
        when(nodeMapper.selectByParentId(0L)).thenReturn(Collections.emptyList());
        when(nodeMapper.selectMaxSortByParent(0L)).thenReturn(null);
        doAnswer(invocation -> {
            CatalogNode node = invocation.getArgument(0);
            node.setId(100L);
            return null;
        }).when(nodeMapper).insert(any(CatalogNode.class));

        Long nodeId = service.addNode(null, "Root");

        ArgumentCaptor<CatalogNode> nodeCaptor = ArgumentCaptor.forClass(CatalogNode.class);
        verify(nodeMapper).insert(nodeCaptor.capture());
        CatalogNode inserted = nodeCaptor.getValue();
        assertThat(nodeId).isEqualTo(100L);
        assertThat(inserted.getParentId()).isEqualTo(0L);
        assertThat(inserted.getLevel()).isEqualTo(1);
        assertThat(inserted.getSort()).isEqualTo(1);
        assertThat(inserted.getName()).isEqualTo("Root");
        verify(nodeMapper).updatePath(100L, "/100");
    }

    @Test
    void bindInsertsRelationForLeafNode() {
        when(nodeMapper.countChildren(10L)).thenReturn(0);
        when(relMapper.selectByBiz("biz-1", "deliver")).thenReturn(Collections.emptyList());

        service.bind(10L, " biz-1 ", " deliver ");

        ArgumentCaptor<CatalogRel> relCaptor = ArgumentCaptor.forClass(CatalogRel.class);
        verify(relMapper).insert(relCaptor.capture());
        CatalogRel inserted = relCaptor.getValue();
        assertThat(inserted.getNodeId()).isEqualTo(10L);
        assertThat(inserted.getBizId()).isEqualTo("biz-1");
        assertThat(inserted.getBizType()).isEqualTo("deliver");
    }

    @Test
    void bindIsIdempotentWhenBizIsAlreadyBoundToSameNode() {
        when(nodeMapper.countChildren(10L)).thenReturn(0);
        when(relMapper.selectByBiz("biz-1", "deliver"))
                .thenReturn(List.of(rel(10L, "biz-1", "deliver")));

        service.bind(10L, "biz-1", "deliver");

        verify(relMapper, never()).insert(any(CatalogRel.class));
    }

    @Test
    void bindRejectsBindingToAnotherNode() {
        when(nodeMapper.countChildren(10L)).thenReturn(0);
        when(relMapper.selectByBiz("biz-1", "deliver"))
                .thenReturn(List.of(rel(20L, "biz-1", "deliver")));

        assertThatThrownBy(() -> service.bind(10L, "biz-1", "deliver"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo("BIZ_ALREADY_BOUND");

        verify(relMapper, never()).insert(any(CatalogRel.class));
    }

    @Test
    void bindRejectsNonLeafNode() {
        when(nodeMapper.countChildren(10L)).thenReturn(1);

        assertThatThrownBy(() -> service.bind(10L, "biz-1", "deliver"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo("NOT_LEAF_NODE");

        verify(relMapper, never()).selectByBiz(anyString(), anyString());
    }

    @Test
    void batchBindRejectsMultipleNodeIdsForOneBiz() {
        assertThatThrownBy(() -> service.batchBind(List.of(10L, 20L), "biz-1", "deliver"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo("INVALID_ARGUMENT");

        verifyNoInteractions(nodeMapper, relMapper);
    }

    @Test
    void batchBindByBizIdsInsertsOnlyNewPairwiseBindings() {
        when(nodeMapper.selectIdsHavingChildren(List.of(10L, 20L))).thenReturn(Collections.emptyList());
        when(relMapper.selectByBizIds(List.of("biz-1", "biz-2"), "deliver"))
                .thenReturn(List.of(rel(10L, "biz-1", "deliver")));

        service.batchBindByBizIds(List.of(10L, 20L), List.of("biz-1", "biz-2"), "deliver");

        ArgumentCaptor<List<CatalogRel>> relsCaptor = ArgumentCaptor.forClass(List.class);
        verify(relMapper).batchInsert(relsCaptor.capture());
        assertThat(relsCaptor.getValue())
                .singleElement()
                .satisfies(rel -> {
                    assertThat(rel.getNodeId()).isEqualTo(20L);
                    assertThat(rel.getBizId()).isEqualTo("biz-2");
                    assertThat(rel.getBizType()).isEqualTo("deliver");
                });
    }

    @Test
    void batchBindByBizIdsRejectsConflictingRequestBindings() {
        when(nodeMapper.selectIdsHavingChildren(List.of(10L, 20L))).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.batchBindByBizIds(
                List.of(10L, 20L),
                List.of("biz-1", "biz-1"),
                "deliver"
        ))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo("INVALID_ARGUMENT");

        verify(relMapper, never()).selectByBizIds(anyList(), anyString());
        verify(relMapper, never()).batchInsert(anyList());
    }

    @Test
    void getBizPathReturnsNodesInPathOrder() {
        when(relMapper.selectByBiz("biz-1", "deliver"))
                .thenReturn(List.of(rel(3L, "biz-1", "deliver")));
        when(nodeMapper.selectById(3L)).thenReturn(node(3L, 2L, "Leaf", "/1/2/3", 3, 1));
        when(nodeMapper.selectByIds(List.of(1L, 2L, 3L))).thenReturn(List.of(
                node(3L, 2L, "Leaf", "/1/2/3", 3, 1),
                node(1L, 0L, "Root", "/1", 1, 1),
                node(2L, 1L, "Project", "/1/2", 2, 1)
        ));

        List<CatalogNode> path = service.getBizPath("biz-1", "deliver");

        assertThat(path).extracting(CatalogNode::getId).containsExactly(1L, 2L, 3L);
    }

    @Test
    void getNodeIdsReturnsSingleBoundNodeId() {
        when(relMapper.selectByBiz("biz-1", "deliver"))
                .thenReturn(List.of(rel(3L, "biz-1", "deliver")));

        List<Long> nodeIds = service.getNodeIds("biz-1", "deliver");

        assertThat(nodeIds).containsExactly(3L);
    }

    @Test
    void listNodesInTreeOrderReturnsNodesInTreeOrderUsingSiblingSort() {
        when(nodeMapper.selectAll()).thenReturn(List.of(
                node(1L, 0L, "Root", "/1", 1, 1),
                node(10L, 1L, "Second", "/1/10", 2, 2),
                node(11L, 10L, "Grandchild", "/1/10/11", 3, 1),
                node(2L, 1L, "First", "/1/2", 2, 1)
        ));

        List<CatalogNode> nodes = service.listNodesInTreeOrder();

        assertThat(nodes).extracting(CatalogNode::getId).containsExactly(1L, 2L, 10L, 11L);
    }

    @Test
    void listSubtreeNodesReturnsNodesInTreeOrderUsingSiblingSort() {
        CatalogNode root = node(1L, 0L, "Root", "/1", 1, 1);
        when(nodeMapper.selectById(1L)).thenReturn(root);
        when(nodeMapper.selectByPathPrefix("/1")).thenReturn(List.of(
                root,
                node(10L, 1L, "Second", "/1/10", 2, 2),
                node(11L, 10L, "Grandchild", "/1/10/11", 3, 1),
                node(2L, 1L, "First", "/1/2", 2, 1)
        ));

        List<CatalogNode> subtree = service.listSubtreeNodes(1L);

        assertThat(subtree).extracting(CatalogNode::getId).containsExactly(1L, 2L, 10L, 11L);
    }

    @Test
    void listBizRelatedNodesReturnsNodesInTreeOrderUsingSiblingSort() {
        when(relMapper.selectByBiz("biz-1", "deliver"))
                .thenReturn(List.of(rel(11L, "biz-1", "deliver")));
        when(nodeMapper.selectById(11L)).thenReturn(node(11L, 10L, "Grandchild", "/1/10/11", 3, 1));
        when(nodeMapper.selectByIds(List.of(11L, 1L, 10L))).thenReturn(List.of(
                node(11L, 10L, "Grandchild", "/1/10/11", 3, 1),
                node(1L, 0L, "Root", "/1", 1, 1),
                node(10L, 1L, "Second", "/1/10", 2, 2)
        ));

        List<CatalogNode> bizTree = service.listBizRelatedNodes("biz-1", "deliver");

        assertThat(bizTree).extracting(CatalogNode::getId).containsExactly(1L, 10L, 11L);
    }

    @Test
    void getBizPathRejectsHistoricalMultipleBindings() {
        when(relMapper.selectByBiz("biz-1", "deliver")).thenReturn(List.of(
                rel(3L, "biz-1", "deliver"),
                rel(4L, "biz-1", "deliver")
        ));

        assertThatThrownBy(() -> service.getBizPath("biz-1", "deliver"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo("BIZ_BOUND_TO_MULTIPLE_NODES");
    }

    @Test
    void getBizTreeRejectsHistoricalMultipleBindings() {
        when(relMapper.selectByBiz("biz-1", "deliver")).thenReturn(List.of(
                rel(3L, "biz-1", "deliver"),
                rel(4L, "biz-1", "deliver")
        ));

        assertThatThrownBy(() -> service.listBizRelatedNodes("biz-1", "deliver"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo("BIZ_BOUND_TO_MULTIPLE_NODES");
    }

    @Test
    void deleteNodeRejectsNonRecursiveDeleteWhenBindingsExist() {
        CatalogNode node = node(10L, 0L, "Leaf", "/10", 1, 1);
        when(nodeMapper.selectById(10L)).thenReturn(node);
        when(nodeMapper.selectByParentId(0L)).thenReturn(List.of(node));
        when(nodeMapper.selectByPathPrefix("/10")).thenReturn(List.of(node));
        when(relMapper.countByNodeIds(List.of(10L))).thenReturn(1);

        assertThatThrownBy(() -> service.deleteNode(10L, false))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo("HAS_BINDINGS");

        verify(relMapper, never()).deleteByNodeIds(anyList());
        verify(nodeMapper, never()).deleteByIds(anyList());
    }

    private CatalogNode node(Long id, Long parentId, String name, String path, Integer level, Integer sort) {
        CatalogNode node = new CatalogNode();
        node.setId(id);
        node.setParentId(parentId);
        node.setName(name);
        node.setPath(path);
        node.setLevel(level);
        node.setSort(sort);
        return node;
    }

    private CatalogRel rel(Long nodeId, String bizId, String bizType) {
        CatalogRel rel = new CatalogRel();
        rel.setNodeId(nodeId);
        rel.setBizId(bizId);
        rel.setBizType(bizType);
        return rel;
    }
}
