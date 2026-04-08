package io.github.zhubn123.catalog.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 目录树节点视图。
 *
 * <p>该对象用于对外返回已经组装好的嵌套树结构，在保留节点基础元数据的同时，
 * 额外提供 {@code children} 方便前端或上层服务直接消费。</p>
 */
@Data
public class CatalogTreeNode {

    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private String path;
    private Integer level;
    private Integer sort;
    /**
     * 是否为当前返回树中的叶子节点。
     *
     * <p>该标记由后端组装树时计算，便于前端直接决定是否展示“可绑定业务对象”等交互。</p>
     */
    private Boolean leaf;

    /**
     * 当前节点是否适合作为业务绑定目标。
     *
     * <p>现阶段默认由后端直接标记为可绑定，后续如果引入更细粒度的绑定策略，
     * 可以在不改接口结构的前提下继续扩展。</p>
     */
    private Boolean bindable;

    /**
     * 预留给扩展策略的附加数据。
     *
     * <p>例如后续可以在目录节点上补充业务摘要、绑定统计或前端展示所需的额外标记。</p>
     */
    private Map<String, Object> extensions = new LinkedHashMap<>();

    private List<CatalogTreeNode> children = new ArrayList<>();

    public static CatalogTreeNode from(CatalogNode node) {
        CatalogTreeNode treeNode = new CatalogTreeNode();
        treeNode.setId(node.getId());
        treeNode.setParentId(node.getParentId());
        treeNode.setName(node.getName());
        treeNode.setCode(node.getCode());
        treeNode.setPath(node.getPath());
        treeNode.setLevel(node.getLevel());
        treeNode.setSort(node.getSort());
        return treeNode;
    }
}
