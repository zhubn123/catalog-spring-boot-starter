package io.github.zhubn123.catalog.domain;

import lombok.Data;

/**
 * 目录节点实体。
 *
 * <p>用于描述目录树中的节点信息，包括父子关系、路径、层级和排序等元数据。</p>
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
