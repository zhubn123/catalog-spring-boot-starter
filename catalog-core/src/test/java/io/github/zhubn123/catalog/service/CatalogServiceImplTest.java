package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogPage;
import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogSortRepairResult;
import io.github.zhubn123.catalog.domain.CatalogTreeNode;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
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
        assertThat(inserted.getSort()).isEqualTo(1024);
        assertThat(inserted.getName()).isEqualTo("Root");
        verify(nodeMapper).updatePath(100L, "/100");
        verify(nodeMapper, never()).selectByParentId(anyLong());
    }

    @Test
    void addNodeAppendsAfterRootMaxSortWithoutScanningAllRootSiblings() {
        when(nodeMapper.selectMaxSortByParent(0L)).thenReturn(10_000);
        doAnswer(invocation -> {
            CatalogNode node = invocation.getArgument(0);
            node.setId(10_001L);
            return null;
        }).when(nodeMapper).insert(any(CatalogNode.class));

        Long nodeId = service.addNode(0L, "Root-10001");

        ArgumentCaptor<CatalogNode> nodeCaptor = ArgumentCaptor.forClass(CatalogNode.class);
        verify(nodeMapper).insert(nodeCaptor.capture());
        assertThat(nodeId).isEqualTo(10_001L);
        assertThat(nodeCaptor.getValue().getSort()).isEqualTo(11_024);
        verify(nodeMapper, never()).selectByParentId(anyLong());
    }

    @Test
    void batchAddNodeAppendsSequentialSortsWithoutScanningAllRootSiblings() {
        when(nodeMapper.selectMaxSortByParent(0L)).thenReturn(10_000);
        doAnswer(invocation -> {
            List<CatalogNode> nodes = invocation.getArgument(0);
            long id = 10_001L;
            for (CatalogNode node : nodes) {
                node.setId(id++);
            }
            return null;
        }).when(nodeMapper).batchInsert(anyList());

        List<Long> nodeIds = service.batchAddNode(0L, new String[]{"A", "B"});

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CatalogNode>> nodesCaptor = ArgumentCaptor.forClass(List.class);
        verify(nodeMapper).batchInsert(nodesCaptor.capture());
        assertThat(nodeIds).containsExactly(10_001L, 10_002L);
        assertThat(nodesCaptor.getValue()).extracting(CatalogNode::getSort).containsExactly(11_024, 12_048);
        verify(nodeMapper, never()).selectByParentId(anyLong());
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
    void batchBindRejectsEmptyNodeIds() {
        assertThatThrownBy(() -> service.batchBind(List.of(), "biz-1", "deliver"))
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
    void listChildrenNodesReturnsDirectChildrenForRootLazyLoading() {
        when(nodeMapper.selectByParentId(0L)).thenReturn(List.of(
                node(1L, 0L, "Root-A", "/1", 1, 1024),
                node(2L, 0L, "Root-B", "/2", 1, 2048)
        ));

        List<CatalogNode> children = service.listChildrenNodes(null);

        verify(nodeMapper).selectByParentId(0L);
        assertThat(children).extracting(CatalogNode::getId).containsExactly(1L, 2L);
    }

    @Test
    void pageChildrenNodesReturnsPagedDirectChildrenForRoot() {
        when(nodeMapper.countByParentId(0L)).thenReturn(21L);
        when(nodeMapper.selectByParentIdPage(0L, 20L, 20)).thenReturn(List.of(
                node(21L, 0L, "Root-21", "/21", 1, 21_504)
        ));

        CatalogPage<CatalogNode> page = service.pageChildrenNodes(null, 2, 20);

        verify(nodeMapper).countByParentId(0L);
        verify(nodeMapper).selectByParentIdPage(0L, 20L, 20);
        assertThat(page.getPage()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.getTotal()).isEqualTo(21L);
        assertThat(page.isHasNext()).isFalse();
        assertThat(page.getItems()).extracting(CatalogNode::getId).containsExactly(21L);
    }

    @Test
    void pageChildrenNodesRejectsOversizedPageSize() {
        assertThatThrownBy(() -> service.pageChildrenNodes(0L, 1, 201))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo("INVALID_ARGUMENT");

        verify(nodeMapper, never()).countByParentId(anyLong());
        verify(nodeMapper, never()).selectByParentIdPage(anyLong(), anyLong(), anyInt());
    }

    @Test
    void listNodeTreeReturnsNestedTreeStructure() {
        when(nodeMapper.selectAll()).thenReturn(List.of(
                node(1L, 0L, "Root", "/1", 1, 1),
                node(10L, 1L, "Second", "/1/10", 2, 2),
                node(11L, 10L, "Grandchild", "/1/10/11", 3, 1),
                node(2L, 1L, "First", "/1/2", 2, 1)
        ));

        List<CatalogTreeNode> tree = service.listNodeTree();

        assertThat(tree).singleElement().satisfies(root -> {
            assertThat(root.getId()).isEqualTo(1L);
            assertThat(root.getChildren()).extracting(CatalogTreeNode::getId).containsExactly(2L, 10L);
            assertThat(root.getChildren().get(1).getChildren())
                    .extracting(CatalogTreeNode::getId)
                    .containsExactly(11L);
        });
    }

    @Test
    void moveNodeReordersSiblingsWithinSameParent() {
        CatalogNode parent = node(1L, 0L, "Root", "/1", 1, 1);
        CatalogNode first = node(10L, 1L, "First", "/1/10", 2, 1024);
        CatalogNode second = node(11L, 1L, "Second", "/1/11", 2, 2048);
        CatalogNode third = node(12L, 1L, "Third", "/1/12", 2, 3072);
        when(nodeMapper.selectById(11L)).thenReturn(second);
        when(nodeMapper.selectById(1L)).thenReturn(parent);
        when(nodeMapper.selectByParentId(1L)).thenReturn(List.of(first, second, third));

        service.moveNode(11L, 1L, 0);

        verify(nodeMapper).updateSort(11L, 512);
        verify(nodeMapper, never()).updateParentLevelPathSort(anyLong(), anyLong(), anyInt(), anyString(), anyInt());
        verify(nodeMapper, never()).moveSubtree(anyString(), anyString(), anyInt());
    }

    @Test
    void moveNodeRebalancesSiblingsWhenNoGapExistsAtTargetPosition() {
        CatalogNode movingNode = node(12L, 1L, "Third", "/1/12", 2, 3);
        CatalogNode first = node(10L, 1L, "First", "/1/10", 2, 1);
        CatalogNode second = node(11L, 1L, "Second", "/1/11", 2, 2);
        when(nodeMapper.selectById(12L)).thenReturn(movingNode);
        when(nodeMapper.selectById(1L)).thenReturn(node(1L, 0L, "Root", "/1", 1, 1));
        when(nodeMapper.selectByParentId(1L)).thenReturn(List.of(first, second, movingNode));

        service.moveNode(12L, 1L, 1);

        verify(nodeMapper).updateSort(10L, 1024);
        verify(nodeMapper).updateSort(11L, 2048);
        verify(nodeMapper).updateSort(12L, 1536);
    }

    @Test
    void moveNodeUpdatesParentAndSubtreeWhenMovingAcrossParents() {
        CatalogNode movingNode = node(11L, 1L, "Second", "/1/11", 2, 2);
        CatalogNode newParent = node(20L, 0L, "Archive", "/20", 1, 1);
        when(nodeMapper.selectById(11L)).thenReturn(movingNode);
        when(nodeMapper.selectById(20L)).thenReturn(newParent);
        when(nodeMapper.selectMaxSortByParent(20L)).thenReturn(null);

        service.moveNode(11L, 20L, null);

        verify(nodeMapper).updateParentLevelPathSort(11L, 20L, 2, "/20/11", 1024);
        verify(nodeMapper).moveSubtree("/1/11", "/20/11", 0);
        verify(nodeMapper, never()).selectByParentId(anyLong());
    }

    @Test
    void moveNodeToRootAppendsAfterRootMaxSortWithoutScanningRootSiblings() {
        CatalogNode movingNode = node(23L, 21L, "测试", "/21/23", 2, 2);
        when(nodeMapper.selectById(23L)).thenReturn(movingNode);
        when(nodeMapper.selectMaxSortByParent(0L)).thenReturn(16);

        service.moveNode(23L, 0L, null);

        verify(nodeMapper).updateParentLevelPathSort(23L, 0L, 1, "/23", 1040);
        verify(nodeMapper).moveSubtree("/21/23", "/23", -1);
        verify(nodeMapper, never()).selectByParentId(anyLong());
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
    void listSubtreeTreeReturnsNestedTreeStructure() {
        CatalogNode root = node(1L, 0L, "Root", "/1", 1, 1);
        when(nodeMapper.selectById(1L)).thenReturn(root);
        when(nodeMapper.selectByPathPrefix("/1")).thenReturn(List.of(
                root,
                node(10L, 1L, "Second", "/1/10", 2, 2),
                node(11L, 10L, "Grandchild", "/1/10/11", 3, 1),
                node(2L, 1L, "First", "/1/2", 2, 1)
        ));

        List<CatalogTreeNode> subtree = service.listSubtreeTree(1L);

        assertThat(subtree).singleElement().satisfies(treeRoot -> {
            assertThat(treeRoot.getId()).isEqualTo(1L);
            assertThat(treeRoot.getChildren()).extracting(CatalogTreeNode::getId).containsExactly(2L, 10L);
        });
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
    void listBizRelatedTreeReturnsNestedTreeStructure() {
        when(relMapper.selectByBiz("biz-1", "deliver"))
                .thenReturn(List.of(rel(11L, "biz-1", "deliver")));
        when(nodeMapper.selectById(11L)).thenReturn(node(11L, 10L, "Grandchild", "/1/10/11", 3, 1));
        when(nodeMapper.selectByIds(List.of(11L, 1L, 10L))).thenReturn(List.of(
                node(11L, 10L, "Grandchild", "/1/10/11", 3, 1),
                node(1L, 0L, "Root", "/1", 1, 1),
                node(10L, 1L, "Second", "/1/10", 2, 2)
        ));

        List<CatalogTreeNode> bizTree = service.listBizRelatedTree("biz-1", "deliver");

        assertThat(bizTree).singleElement().satisfies(root -> {
            assertThat(root.getId()).isEqualTo(1L);
            assertThat(root.getChildren()).singleElement().satisfies(second -> {
                assertThat(second.getId()).isEqualTo(10L);
                assertThat(second.getChildren()).extracting(CatalogTreeNode::getId).containsExactly(11L);
            });
        });
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
        when(nodeMapper.selectByPathPrefix("/10")).thenReturn(List.of(node));
        when(relMapper.countByNodeIds(List.of(10L))).thenReturn(1);

        assertThatThrownBy(() -> service.deleteNode(10L, false))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo("HAS_BINDINGS");

        verify(relMapper, never()).deleteByNodeIds(anyList());
        verify(nodeMapper, never()).deleteByIds(anyList());
        verify(nodeMapper, never()).selectByParentId(anyLong());
    }

    @Test
    void repairSiblingSortsRebuildsSingleParentOrdering() {
        when(nodeMapper.selectByParentId(0L)).thenReturn(List.of(
                node(3L, 0L, "Root-C", "/3", 1, 4096),
                node(1L, 0L, "Root-A", "/1", 1, 1024),
                node(2L, 0L, "Root-B", "/2", 1, 4097)
        ));

        CatalogSortRepairResult result = service.repairSiblingSorts(0L);

        verify(nodeMapper).updateSort(3L, 2048);
        verify(nodeMapper).updateSort(2L, 3072);
        assertThat(result.getScope()).isEqualTo("PARENT");
        assertThat(result.getParentId()).isEqualTo(0L);
        assertThat(result.getUpdatedNodes()).isEqualTo(2);
    }

    @Test
    void repairAllSiblingSortsRebuildsEverySiblingGroup() {
        when(nodeMapper.selectAll()).thenReturn(List.of(
                node(1L, 0L, "Root-A", "/1", 1, 1024),
                node(2L, 0L, "Root-B", "/2", 1, 4096),
                node(3L, 1L, "Child-A", "/1/3", 2, 4096),
                node(4L, 1L, "Child-B", "/1/4", 2, 8192)
        ));

        CatalogSortRepairResult result = service.repairAllSiblingSorts();

        verify(nodeMapper).updateSort(2L, 2048);
        verify(nodeMapper).updateSort(3L, 1024);
        verify(nodeMapper).updateSort(4L, 2048);
        assertThat(result.getScope()).isEqualTo("ALL");
        assertThat(result.getGroups()).isEqualTo(2);
        assertThat(result.getScannedNodes()).isEqualTo(4);
        assertThat(result.getUpdatedNodes()).isEqualTo(3);
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
