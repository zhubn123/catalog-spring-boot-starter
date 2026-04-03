package com.berlin.catalog.domain;

import lombok.Data;

/**
 * 目录-业务关系实体类
 * 
 * <p>该类表示目录节点与业务对象之间的绑定关系，是连接目录树结构与业务数据的桥梁。
 * 通过该关系表，可以实现业务对象在目录树中的灵活挂载和快速检索。</p>
 * 
 * <h3>核心设计原则：</h3>
 * <ul>
 *   <li><b>叶子节点绑定</b>：只有叶子节点（无子节点的节点）才能绑定业务对象</li>
 *   <li><b>多对多关系</b>：一个业务对象可以绑定到多个节点，一个节点也可以绑定多个业务对象</li>
 *   <li><b>类型区分</b>：通过 bizType 区分不同类型的业务对象（如合同、交付物、任务等）</li>
 * </ul>
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
