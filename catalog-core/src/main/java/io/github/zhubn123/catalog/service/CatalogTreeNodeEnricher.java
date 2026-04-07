package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogTreeNode;

import java.util.List;

/**
 * 目录树节点增强策略。
 *
 * <p>默认树接口会返回标准节点字段、{@code children}、{@code leaf} 与 {@code bindable}。
 * 如果业务方希望进一步在树节点上补充摘要、统计或挂载信息，可以实现该接口，
 * 把附加内容写入 {@link CatalogTreeNode#getExtensions()}。</p>
 *
 * <p>为了兼顾简单场景和批量优化场景，接口同时提供两种扩展方式：</p>
 *
 * <ul>
 *     <li>直接实现 {@link #enrich(CatalogTreeNode, CatalogNode, CatalogTreeAssembleContext)}，适合轻量单节点增强。</li>
 *     <li>覆写 {@link #prepare(List, CatalogTreeAssembleContext)} 与
 *     {@link #enrich(CatalogTreeNode, CatalogNode, CatalogTreeAssembleContext, Object)}，
 *     适合先批量预取数据，再逐节点组装。</li>
 * </ul>
 */
@FunctionalInterface
public interface CatalogTreeNodeEnricher {

    /**
     * 对单个树节点执行附加组装。
     *
     * <p>建议保持幂等、轻量，并尽量只补充调用方真正需要的字段。</p>
     */
    void enrich(CatalogTreeNode treeNode, CatalogNode sourceNode, CatalogTreeAssembleContext context);

    /**
     * 在一次树组装开始前批量准备扩展所需数据。
     *
     * <p>默认返回 {@code null}。需要避免 N+1 查询时，可以在这里一次性预取所需数据，
     * 再通过带 {@code preparedState} 参数的 {@code enrich} 方法逐节点消费。</p>
     */
    default Object prepare(List<CatalogNode> sourceNodes, CatalogTreeAssembleContext context) {
        return null;
    }

    /**
     * 使用预取结果对单个树节点执行附加组装。
     *
     * <p>默认回落到简单版 {@link #enrich(CatalogTreeNode, CatalogNode, CatalogTreeAssembleContext)}，
     * 这样现有基于 lambda 的实现无需改动也能继续工作。</p>
     */
    default void enrich(
            CatalogTreeNode treeNode,
            CatalogNode sourceNode,
            CatalogTreeAssembleContext context,
            Object preparedState
    ) {
        enrich(treeNode, sourceNode, context);
    }
}
