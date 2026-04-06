package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogTreeNode;

/**
 * 目录树节点增强策略。
 *
 * <p>默认树接口会返回标准节点字段、{@code children}、{@code leaf} 与 {@code bindable}。
 * 如果业务方希望进一步在树节点上补充叶子节点绑定信息、展示摘要或统计数据，
 * 可以通过实现该接口，把附加内容写入 {@link CatalogTreeNode#getExtensions()}。</p>
 */
@FunctionalInterface
public interface CatalogTreeNodeEnricher {

    /**
     * 对单个树节点执行附加组装。
     *
     * <p>建议保持幂等、轻量，并尽量只补充调用方真正需要的字段。</p>
     */
    void enrich(CatalogTreeNode treeNode, CatalogNode sourceNode, CatalogTreeAssembleContext context);
}
