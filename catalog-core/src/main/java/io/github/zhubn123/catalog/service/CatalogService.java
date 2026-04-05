package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;

import java.util.List;

/**
 * 目录领域服务接口。
 *
 * <p>定义目录节点管理、业务绑定以及查询相关的核心能力。</p>
 *
 * @author zhubn
 * @date 2026/4/2
 */
public interface CatalogService {

    Long addNode(Long parentId, String name);

    List<Long> batchAddNode(Long parentId, String[] names);

    void moveNode(Long nodeId, Long parentId);

    void moveNode(Long nodeId, Long parentId, Integer targetIndex);

    void updateNode(Long nodeId, String name, String code, Integer sort);

    void deleteNode(Long nodeId, boolean recursive);

    void bind(Long nodeId, String bizId, String bizType);

    void batchBind(List<Long> nodeIds, String bizId, String bizType);

    void batchBindByBizIds(List<Long> nodeIds, List<String> bizIds, String bizType);

    void unbind(Long nodeId, String bizId, String bizType);

    List<CatalogNode> tree();

    List<CatalogNode> getBizPath(String bizId, String bizType);

    List<Long> getNodeIds(String bizId, String bizType);

    List<String> getBizIds(Long nodeId, String bizType);

    List<String> getBizIdsByNodeTree(Long nodeId, String bizType);

    List<CatalogNode> getBizTree(String bizId, String bizType);

    List<CatalogNode> getSubtree(Long nodeId);
}
