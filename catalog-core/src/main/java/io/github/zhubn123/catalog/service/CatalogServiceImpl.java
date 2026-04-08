package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogTreeNode;
import io.github.zhubn123.catalog.mapper.CatalogNodeMapper;
import io.github.zhubn123.catalog.mapper.CatalogRelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 目录服务门面实现。
 *
 * <p>对外仍然只暴露一个 {@link CatalogService}，内部将节点命令、业务绑定与查询逻辑
 * 分别委托给更聚焦的协作类，降低单类复杂度。</p>
 *
 * @author zhubn
 * @date 2026/4/2
 */
@Service
public class CatalogServiceImpl implements CatalogService {

    private final CatalogNodeCommandService nodeCommandService;
    private final CatalogBindingService bindingService;
    private final CatalogQueryService queryService;

    public CatalogServiceImpl(CatalogNodeMapper nodeMapper, CatalogRelMapper relMapper) {
        this(nodeMapper, relMapper, List.of());
    }

    public CatalogServiceImpl(
            CatalogNodeMapper nodeMapper,
            CatalogRelMapper relMapper,
            List<CatalogTreeNodeEnricher> treeNodeEnrichers
    ) {
        this.nodeCommandService = new CatalogNodeCommandService(nodeMapper, relMapper);
        this.bindingService = new CatalogBindingService(nodeMapper, relMapper);
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
    public List<CatalogNode> listNodesInTreeOrder() {
        return queryService.listNodesInTreeOrder();
    }

    @Override
    public List<CatalogNode> listChildrenNodes(Long parentId) {
        return queryService.listChildrenNodes(parentId);
    }

    @Override
    public List<CatalogTreeNode> listNodeTree() {
        return queryService.listNodeTree();
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
