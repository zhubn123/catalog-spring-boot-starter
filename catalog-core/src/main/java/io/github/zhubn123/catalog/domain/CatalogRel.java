package io.github.zhubn123.catalog.domain;

import lombok.Data;

/**
 * 目录与业务对象的绑定关系实体。
 *
 * <p>用于描述目录节点和业务对象之间的关联关系。</p>
 *
 * @author zhubn
 * @date 2026/4/2
 * @see CatalogNode 目录节点实体
 */
@Data
public class CatalogRel {

    private Long id;
    private Long nodeId;
    private String bizId;
    private String bizType;
}
