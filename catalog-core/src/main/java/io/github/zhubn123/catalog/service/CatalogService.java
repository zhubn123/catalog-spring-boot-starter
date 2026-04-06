package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogTreeNode;

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

    /**
     * 将业务对象绑定到单个叶子节点。
     *
     * <p>目录节点可以只作为容器，不要求一定绑定业务对象；但同一个
     * {@code bizType + bizId} 最多只能绑定一个目录节点。</p>
     */
    void bind(Long nodeId, String bizId, String bizType);

    /**
     * 兼容保留的批量绑定入口。
     *
     * <p>单个业务对象只能绑定一个节点，因此该方法仅接受单个有效节点 ID。
     * 多组一对一绑定请使用 {@link #batchBindByBizIds(List, List, String)}。</p>
     */
    @Deprecated
    void batchBind(List<Long> nodeIds, String bizId, String bizType);

    /**
     * 按顺序批量执行“一对一”业务绑定。
     *
     * <p>{@code nodeIds} 与 {@code bizIds} 需要一一对应，适用于批量创建叶子节点后，
     * 再将多个业务对象分别绑定到各自节点的场景。</p>
     */
    void batchBindByBizIds(List<Long> nodeIds, List<String> bizIds, String bizType);

    void unbind(Long nodeId, String bizId, String bizType);

    /**
     * 按树遍历顺序返回全量节点列表。
     *
     * <p>该方法返回的是扁平节点列表，而不是已经组装好的嵌套树结构。</p>
     */
    List<CatalogNode> listNodesInTreeOrder();

    /**
     * 返回完整目录的嵌套树结构。
     *
     * <p>与 {@link #listNodesInTreeOrder()} 不同，该方法会在后端完成父子关系组装，
     * 适合前端直接展示树形组件。</p>
     */
    List<CatalogTreeNode> listNodeTree();

    /**
     * 兼容旧命名，仍然返回按树遍历顺序排列的扁平节点列表。
     */
    @Deprecated
    default List<CatalogNode> tree() {
        return listNodesInTreeOrder();
    }

    /**
     * 查询业务对象绑定节点的唯一路径。
     *
     * <p>当业务对象未绑定任何节点时返回空列表；如果历史数据出现多节点绑定，
     * 将抛出异常而不是静默返回其中一条路径。</p>
     */
    List<CatalogNode> getBizPath(String bizId, String bizType);

    List<Long> getNodeIds(String bizId, String bizType);

    List<String> getBizIds(Long nodeId, String bizType);

    List<String> getBizIdsByNodeTree(Long nodeId, String bizType);

    /**
     * 返回用于还原业务局部树的节点列表。
     *
     * <p>结果包含绑定节点及其祖先节点，返回值仍然是按树遍历顺序排列的扁平列表。</p>
     */
    List<CatalogNode> listBizRelatedNodes(String bizId, String bizType);

    /**
     * 返回业务对象对应的局部嵌套树结构。
     *
     * <p>结果包含绑定节点及其祖先节点，并在后端完成 {@code children} 组装。</p>
     */
    List<CatalogTreeNode> listBizRelatedTree(String bizId, String bizType);

    /**
     * 兼容旧命名，仍然返回用于还原业务局部树的扁平节点列表。
     */
    @Deprecated
    default List<CatalogNode> getBizTree(String bizId, String bizType) {
        return listBizRelatedNodes(bizId, bizType);
    }

    /**
     * 返回指定节点子树的节点列表。
     *
     * <p>结果包含当前节点及其全部后代节点，返回值仍然是按树遍历顺序排列的扁平列表。</p>
     */
    List<CatalogNode> listSubtreeNodes(Long nodeId);

    /**
     * 返回指定节点子树的嵌套树结构。
     *
     * <p>结果包含当前节点及其全部后代节点，并按树结构组装为多层 children。</p>
     */
    List<CatalogTreeNode> listSubtreeTree(Long nodeId);

    /**
     * 兼容旧命名，仍然返回子树节点的扁平列表。
     */
    @Deprecated
    default List<CatalogNode> getSubtree(Long nodeId) {
        return listSubtreeNodes(nodeId);
    }
}
