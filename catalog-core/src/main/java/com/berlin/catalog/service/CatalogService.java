package com.berlin.catalog.service;

import com.berlin.catalog.domain.CatalogNode;

import java.util.List;

/**
 * 目录领域服务接口
 * 
 * <p>该接口定义了目录树管理的核心领域服务，提供目录节点的生命周期管理、
 * 业务绑定关系管理以及多种查询能力。是整个catalog模块的核心接口。</p>
 * 
 * <h2>核心数据模型</h2>
 * <ul>
 *   <li><b>catalog_node</b>：目录节点表，存储树形结构元数据（父节点、路径、层级、排序）</li>
 *   <li><b>catalog_rel</b>：业务绑定表，存储节点与业务对象的关联关系，仅支持叶子节点</li>
 * </ul>
 * 
 * <h2>推荐业务流程</h2>
 * <pre>
 * 1. 创建目录节点
 *    - 使用 addNode() 或 batchAddNode() 创建文件夹结构
 *    - 可先创建临时节点，后续再移动到目标位置
 * 
 * 2. 调整目录结构（可选）
 *    - 使用 moveNode() 移动节点到新位置
 *    - 支持拖拽排序，指定目标位置索引
 * 
 * 3. 绑定业务对象
 *    - 使用 bind() 或 batchBind*() 将业务ID绑定到叶子节点
 *    - 注意：只有叶子节点才能绑定业务对象
 * 
 * 4. 查询与渲染
 *    - 使用 tree() 获取完整树结构
 *    - 使用 getBizPath() 获取业务对象的目录路径
 *    - 使用 getBizTree() 获取业务相关的子树
 * </pre>
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
