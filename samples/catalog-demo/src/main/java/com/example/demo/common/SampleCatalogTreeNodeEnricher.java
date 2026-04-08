package com.example.demo.common;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.domain.CatalogRel;
import io.github.zhubn123.catalog.domain.CatalogTreeNode;
import io.github.zhubn123.catalog.mapper.CatalogRelMapper;
import io.github.zhubn123.catalog.service.CatalogTreeAssembleContext;
import io.github.zhubn123.catalog.service.CatalogTreeNodeEnricher;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * sample 项目的树节点扩展示例。
 *
 * <p>这里把节点的直接业务绑定摘要写入 {@code extensions.bindingSummary}，
 * 让前端示例能直观看到扩展点的用法。</p>
 *
 * <p>为了避免 sample 在整棵树里对每个已绑定节点都单独查一次数据库，
 * 这里使用 {@link CatalogTreeNodeEnricher#prepare(List, CatalogTreeAssembleContext)}
 * 先把当前树涉及的节点绑定关系一次性取回，再在逐节点增强阶段消费。</p>
 */
@Component
public class SampleCatalogTreeNodeEnricher implements CatalogTreeNodeEnricher {

    private final CatalogRelMapper catalogRelMapper;

    public SampleCatalogTreeNodeEnricher(CatalogRelMapper catalogRelMapper) {
        this.catalogRelMapper = catalogRelMapper;
    }

    @Override
    public Object prepare(List<CatalogNode> sourceNodes, CatalogTreeAssembleContext context) {
        List<Long> nodeIds = sourceNodes.stream()
                .filter(Objects::nonNull)
                .map(CatalogNode::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (nodeIds.isEmpty()) {
            return Map.of();
        }

        return catalogRelMapper.selectByNodeIds(nodeIds).stream()
                .collect(Collectors.groupingBy(CatalogRel::getNodeId, LinkedHashMap::new, Collectors.toList()));
    }

    @Override
    public void enrich(CatalogTreeNode treeNode, CatalogNode sourceNode, CatalogTreeAssembleContext context) {
        // 兼容未使用预取能力的调用路径；正常情况下，树组装器会优先走带 preparedState 的重载方法。
        applyBindingSummary(treeNode, catalogRelMapper.selectByNode(sourceNode.getId(), null), context);
    }

    @Override
    public void enrich(
            CatalogTreeNode treeNode,
            CatalogNode sourceNode,
            CatalogTreeAssembleContext context,
            Object preparedState
    ) {
        @SuppressWarnings("unchecked")
        Map<Long, List<CatalogRel>> bindingsByNodeId = preparedState instanceof Map<?, ?>
                ? (Map<Long, List<CatalogRel>>) preparedState
                : Map.of();
        List<CatalogRel> bindings = bindingsByNodeId.getOrDefault(sourceNode.getId(), List.of());
        applyBindingSummary(treeNode, bindings, context);
    }

    private void applyBindingSummary(CatalogTreeNode treeNode, List<CatalogRel> bindings, CatalogTreeAssembleContext context) {
        if (treeNode == null) {
            return;
        }

        List<CatalogRel> effectiveBindings = bindings == null ? List.of() : bindings;
        Map<String, Object> bindingSummary = new LinkedHashMap<>();
        bindingSummary.put("count", effectiveBindings.size());
        bindingSummary.put("bizTypes", effectiveBindings.stream().map(CatalogRel::getBizType).distinct().toList());
        bindingSummary.put("bizIds", effectiveBindings.stream().map(CatalogRel::getBizId).toList());
        bindingSummary.put(
                "directBindings",
                effectiveBindings.stream()
                        .map(binding -> Map.of(
                                "bizId", binding.getBizId(),
                                "bizType", binding.getBizType()
                        ))
                        .toList()
        );

        if (context != null && context.scene() != null) {
            bindingSummary.put("scene", context.scene().name());
        }

        treeNode.getExtensions().put("bindingSummary", bindingSummary);
    }
}
