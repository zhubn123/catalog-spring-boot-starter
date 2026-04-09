package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogPage;
import io.github.zhubn123.catalog.domain.CatalogSortRepairResult;
import io.github.zhubn123.catalog.domain.CatalogTreeNode;
import io.github.zhubn123.catalog.mapper.CatalogNodeMapper;
import io.github.zhubn123.catalog.mapper.CatalogRelMapper;
import io.github.zhubn123.catalog.service.binding.CatalogBindingService;
import io.github.zhubn123.catalog.service.command.CatalogNodeCommandService;
import io.github.zhubn123.catalog.service.query.CatalogQueryService;
import io.github.zhubn123.catalog.service.sort.GapCatalogSortStrategy;
import io.github.zhubn123.catalog.service.tree.CatalogTreeAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 目录服务门面实现。
 */
@Service
public class CatalogServiceImpl implements CatalogService {

    private final CatalogNodeCommandService nodeCommandService;
    private final CatalogBindingService bindingService;
    private final CatalogQueryService queryService;

    public CatalogServiceImpl(CatalogNodeMapper nodeMapper, CatalogRelMapper relMapper) {
        this(nodeMapper, relMapper, List.of(), new GapCatalogSortStrategy());
    }

    public CatalogServiceImpl(
            CatalogNodeMapper nodeMapper,
            CatalogRelMapper relMapper,
            List<CatalogTreeNodeEnricher> treeNodeEnrichers
    ) {
        this(nodeMapper, relMapper, treeNodeEnrichers, new GapCatalogSortStrategy());
    }

    public CatalogServiceImpl(
            CatalogNodeMapper nodeMapper,
            CatalogRelMapper relMapper,
            List<CatalogTreeNodeEnricher> treeNodeEnrichers,
            CatalogSortStrategy sortStrategy
    ) {
        this.nodeCommandService = new CatalogNodeCommandService(nodeMapper, relMapper, sortStrategy);
        this.bindingService = new CatalogBindingService(relMapper);
        this.queryService = new CatalogQueryService(nodeMapper, relMapper, new CatalogTreeAssembler(treeNodeEnrichers));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addNode(Long parentId, String name) {
        return nodeCommandService.addNode(parentId, name);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchAddNode(Long parentId, String[] names) {
        return nodeCommandService.batchAddNode(parentId, names);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNode(Long nodeId, String name, String code, Integer sort) {
        nodeCommandService.updateNode(nodeId, name, code, sort);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveNode(Long nodeId, Long parentId) {
        nodeCommandService.moveNode(nodeId, parentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveNode(Long nodeId, Long parentId, Integer targetIndex) {
        nodeCommandService.moveNode(nodeId, parentId, targetIndex);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNode(Long nodeId, boolean recursive) {
        nodeCommandService.deleteNode(nodeId, recursive);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CatalogSortRepairResult repairSiblingSorts(Long parentId) {
        return nodeCommandService.repairSiblingSorts(parentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CatalogSortRepairResult repairAllSiblingSorts() {
        return nodeCommandService.repairAllSiblingSorts();
    }

    @Override
    public void bind(Long nodeId, String bizId, String bizType) {
        bindingService.bind(nodeId, bizId, bizType);
    }

    @Deprecated
    @Override
    public void batchBind(List<Long> nodeIds, String bizId, String bizType) {
        bindingService.batchBind(nodeIds, bizId, bizType);
    }

    @Override
    public void batchBindByBizIds(List<Long> nodeIds, List<String> bizIds, String bizType) {
        bindingService.batchBindByBizIds(nodeIds, bizIds, bizType);
    }

    @Override
    public void unbind(Long nodeId, String bizId, String bizType) {
        bindingService.unbind(nodeId, bizId, bizType);
    }

    @Override
    public List<CatalogNode> listChildrenNodes(Long parentId) {
        return queryService.listChildrenNodes(parentId);
    }

    @Override
    public CatalogPage<CatalogNode> pageChildrenNodes(Long parentId, Integer page, Integer size) {
        return queryService.pageChildrenNodes(parentId, page, size);
    }

    @Override
    public List<CatalogNode> getBizPath(String bizId, String bizType) {
        Long boundNodeId = bindingService.resolveSingleBoundNodeId(bizId, bizType);
        if (boundNodeId == null) {
            return Collections.emptyList();
        }
        return queryService.listPathByNodeId(boundNodeId);
    }

    @Override
    public List<Long> getNodeIds(String bizId, String bizType) {
        Long boundNodeId = bindingService.resolveSingleBoundNodeId(bizId, bizType);
        if (boundNodeId == null) {
            return Collections.emptyList();
        }
        return List.of(boundNodeId);
    }

    @Override
    public List<String> getBizIds(Long nodeId, String bizType) {
        return queryService.getBizIds(nodeId, bizType);
    }

    @Override
    public List<String> getBizIdsByNodeTree(Long nodeId, String bizType) {
        return queryService.getBizIdsByNodeTree(nodeId, bizType);
    }

    @Override
    public List<CatalogNode> listBizRelatedNodes(String bizId, String bizType) {
        Long boundNodeId = bindingService.resolveSingleBoundNodeId(bizId, bizType);
        if (boundNodeId == null) {
            return Collections.emptyList();
        }
        return queryService.listBizRelatedNodes(boundNodeId);
    }

    @Override
    public List<CatalogTreeNode> listBizRelatedTree(String bizId, String bizType) {
        Long boundNodeId = bindingService.resolveSingleBoundNodeId(bizId, bizType);
        if (boundNodeId == null) {
            return Collections.emptyList();
        }
        return queryService.listBizRelatedTree(boundNodeId, bizId, bizType);
    }

    @Override
    public List<CatalogNode> listSubtreeNodes(Long nodeId) {
        return queryService.listSubtreeNodes(nodeId);
    }

    @Override
    public List<CatalogTreeNode> listSubtreeTree(Long nodeId) {
        return queryService.listSubtreeTree(nodeId);
    }
}
