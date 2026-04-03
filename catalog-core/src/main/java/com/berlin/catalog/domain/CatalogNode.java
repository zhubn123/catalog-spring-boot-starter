package com.berlin.catalog.domain;

import lombok.Data;

/**
 * 目录节点实体类
 * 
 * <p>该类表示目录树中的一个节点，用于存储目录的层级结构元数据。
 * 目录树采用经典的父子关系模型，通过 parentId 关联父节点，
 * 同时使用 path 字段存储从根节点到当前节点的完整路径，便于快速查询子树。</p>
 * 
 * <h3>核心设计要点：</h3>
 * <ul>
 *   <li><b>树结构存储</b>：通过 parentId 建立父子关系，支持无限层级</li>
 *   <li><b>路径冗余</b>：path 字段存储完整路径（如 "/1/2/3"），用于高效查询子树</li>
 *   <li><b>排序支持</b>：sort 字段支持同级节点的自定义排序，实现拖拽排序功能</li>
 *   <li><b>业务分离</b>：节点本身不存储业务数据，业务绑定通过 {@link CatalogRel} 实现</li>
 * </ul>
 * 
 * @author zhubn
 * @date 2026/4/2
 * @see CatalogRel 业务绑定关系实体
 */
@Data
public class CatalogNode {

    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private String path;
    private Integer level;
    private Integer sort;
}
