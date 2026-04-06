package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogRel;
import io.github.zhubn123.catalog.exception.CatalogException;
import io.github.zhubn123.catalog.mapper.CatalogNodeMapper;
import io.github.zhubn123.catalog.mapper.CatalogRelMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 负责业务对象与目录节点之间的绑定逻辑。
 */
final class CatalogBindingService {

    private final CatalogNodeMapper nodeMapper;
    private final CatalogRelMapper relMapper;

    CatalogBindingService(CatalogNodeMapper nodeMapper, CatalogRelMapper relMapper) {
        this.nodeMapper = nodeMapper;
        this.relMapper = relMapper;
    }

    void bind(Long nodeId, String bizId, String bizType) {
        validateBindArgs(nodeId, bizId, bizType);
        ensureLeafNode(nodeId);

        String normalizedBizId = trimToNull(bizId);
        String normalizedBizType = trimToNull(bizType);
        Long existingNodeId = resolveSingleBoundNodeId(normalizedBizId, normalizedBizType);
        if (existingNodeId != null) {
            if (Objects.equals(existingNodeId, nodeId)) {
                return;
            }
            throw CatalogException.bizAlreadyBound(normalizedBizId, normalizedBizType, existingNodeId);
        }

        CatalogRel rel = new CatalogRel();
        rel.setNodeId(nodeId);
        rel.setBizId(normalizedBizId);
        rel.setBizType(normalizedBizType);
        relMapper.insert(rel);
    }

    @Deprecated
    void batchBind(List<Long> nodeIds, String bizId, String bizType) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            throw CatalogException.invalidArgument("nodeIds不能为空");
        }

        List<Long> validNodeIds = nodeIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (validNodeIds.isEmpty()) {
            throw CatalogException.invalidArgument("nodeIds不能为空");
        }
        if (validNodeIds.size() > 1) {
            throw CatalogException.invalidArgument(
                    "单个业务对象只能绑定一个节点；如需批量绑定多个业务对象，请使用一对一批量绑定能力"
            );
        }
        bind(validNodeIds.get(0), bizId, bizType);
    }

    void batchBindByBizIds(List<Long> nodeIds, List<String> bizIds, String bizType) {
        if (nodeIds == null || bizIds == null || nodeIds.size() != bizIds.size()) {
            throw CatalogException.invalidArgument("nodeIds和bizIds长度必须相同");
        }
        if (nodeIds.isEmpty()) {
            return;
        }

        ensureLeafNodes(nodeIds);

        String normalizedBizType = trimToNull(bizType);
        if (normalizedBizType == null) {
            throw CatalogException.invalidArgument("bizType不能为空");
        }

        Map<String, Long> requestedBindings = new LinkedHashMap<>();
        for (int i = 0; i < nodeIds.size(); i++) {
            Long nodeId = nodeIds.get(i);
            String bizId = trimToNull(bizIds.get(i));
            if (nodeId == null || nodeId <= 0 || bizId == null) {
                continue;
            }
            Long previousNodeId = requestedBindings.putIfAbsent(bizId, nodeId);
            if (previousNodeId != null && !Objects.equals(previousNodeId, nodeId)) {
                throw CatalogException.invalidArgument("同一个业务对象不能批量绑定到多个节点: " + bizId);
            }
        }

        if (requestedBindings.isEmpty()) {
            return;
        }

        Map<String, Long> existingBindings = resolveExistingBindingsByBizId(
                new ArrayList<>(requestedBindings.keySet()),
                normalizedBizType
        );

        List<CatalogRel> rels = new ArrayList<>(requestedBindings.size());
        for (Map.Entry<String, Long> entry : requestedBindings.entrySet()) {
            String bizId = entry.getKey();
            Long nodeId = entry.getValue();
            Long existingNodeId = existingBindings.get(bizId);
            if (existingNodeId != null) {
                if (Objects.equals(existingNodeId, nodeId)) {
                    continue;
                }
                throw CatalogException.bizAlreadyBound(bizId, normalizedBizType, existingNodeId);
            }

            CatalogRel rel = new CatalogRel();
            rel.setNodeId(nodeId);
            rel.setBizId(bizId);
            rel.setBizType(normalizedBizType);
            rels.add(rel);
        }

        if (!rels.isEmpty()) {
            relMapper.batchInsert(rels);
        }
    }

    void unbind(Long nodeId, String bizId, String bizType) {
        validateBindArgs(nodeId, bizId, bizType);
        relMapper.delete(nodeId, trimToNull(bizId), trimToNull(bizType));
    }

    Long resolveSingleBoundNodeId(String bizId, String bizType) {
        String normalizedBizId = trimToNull(bizId);
        String normalizedBizType = trimToNull(bizType);
        if (normalizedBizId == null || normalizedBizType == null) {
            return null;
        }

        List<Long> nodeIds = relMapper.selectByBiz(normalizedBizId, normalizedBizType).stream()
                .map(CatalogRel::getNodeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (nodeIds.isEmpty()) {
            return null;
        }
        if (nodeIds.size() > 1) {
            throw CatalogException.bizBoundToMultipleNodes(normalizedBizId, normalizedBizType, nodeIds);
        }
        return nodeIds.get(0);
    }

    private void validateBindArgs(Long nodeId, String bizId, String bizType) {
        if (nodeId == null || nodeId <= 0) {
            throw CatalogException.invalidArgument("节点ID无效");
        }
        if (!StringUtils.hasText(bizId)) {
            throw CatalogException.invalidArgument("bizId不能为空");
        }
        if (!StringUtils.hasText(bizType)) {
            throw CatalogException.invalidArgument("bizType不能为空");
        }
    }

    private void ensureLeafNode(Long nodeId) {
        Integer childCount = nodeMapper.countChildren(nodeId);
        if (childCount != null && childCount > 0) {
            throw CatalogException.notLeafNode(nodeId);
        }
    }

    private void ensureLeafNodes(List<Long> nodeIds) {
        List<Long> validIds = nodeIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (validIds.isEmpty()) {
            throw CatalogException.invalidArgument("节点ID列表为空");
        }

        List<Long> nonLeafIds = nodeMapper.selectIdsHavingChildren(validIds);
        if (nonLeafIds != null && !nonLeafIds.isEmpty()) {
            throw CatalogException.notLeafNode(nonLeafIds.get(0));
        }
    }

    private Map<String, Long> resolveExistingBindingsByBizId(List<String> bizIds, String bizType) {
        List<String> normalizedBizIds = bizIds.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedBizIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<Long>> nodeIdsByBizId = relMapper.selectByBizIds(normalizedBizIds, bizType).stream()
                .filter(item -> item.getBizId() != null)
                .collect(Collectors.groupingBy(
                        CatalogRel::getBizId,
                        LinkedHashMap::new,
                        Collectors.mapping(CatalogRel::getNodeId, Collectors.toList())
                ));

        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Long>> entry : nodeIdsByBizId.entrySet()) {
            List<Long> distinctNodeIds = entry.getValue().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (distinctNodeIds.isEmpty()) {
                continue;
            }
            if (distinctNodeIds.size() > 1) {
                throw CatalogException.bizBoundToMultipleNodes(entry.getKey(), bizType, distinctNodeIds);
            }
            result.put(entry.getKey(), distinctNodeIds.get(0));
        }
        return result;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
