package io.github.zhubn123.catalog.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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
