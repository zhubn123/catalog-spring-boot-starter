package com.berlin.catalog.autoconfigure;

import com.berlin.catalog.domain.CatalogNode;
import com.berlin.catalog.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 目录管理REST API控制器
 * 
 * <p>提供目录树操作的RESTful API，可通过配置关闭。</p>
 * 
 * @author zhubn
 * @date 2026/4/2
 */
@RestController
@RequestMapping("/catalog")
public class CatalogController {

    @Autowired
    private CatalogService catalogService;

    @PostMapping("/node")
    public Long addNode(Long parentId, String name) {
        return catalogService.addNode(parentId, name);
    }

    @PostMapping("/node/batch")
    public List<Long> batchAddNode(Long parentId, String names) {
        if (names == null || names.isBlank()) {
            return List.of();
        }
        String[] nameArray = names.split(",");
        return catalogService.batchAddNode(parentId, nameArray);
    }

    @PostMapping("/move")
    public void move(Long nodeId, Long parentId, Integer targetIndex) {
        catalogService.moveNode(nodeId, parentId, targetIndex);
    }

    @PostMapping("/node/update")
    public void updateNode(Long nodeId, String name, String code, Integer sort) {
        catalogService.updateNode(nodeId, name, code, sort);
    }

    @PostMapping("/node/delete")
    public void deleteNode(Long nodeId, Boolean recursive) {
        catalogService.deleteNode(nodeId, Boolean.TRUE.equals(recursive));
    }

    @PostMapping("/bind")
    public void bind(Long nodeId, String bizId, String bizType) {
        catalogService.bind(nodeId, bizId, bizType);
    }

    @PostMapping("/bind/batch")
    public void batchBind(String nodeIds, String bizId, String bizType) {
        if (nodeIds == null || nodeIds.isBlank()) {
            return;
        }
        List<Long> nodeIdList = java.util.Arrays.stream(nodeIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        catalogService.batchBind(nodeIdList, bizId, bizType);
    }

    @PostMapping("/unbind")
    public void unbind(Long nodeId, String bizId, String bizType) {
        catalogService.unbind(nodeId, bizId, bizType);
    }

    @GetMapping("/tree")
    public List<CatalogNode> tree() {
        return catalogService.tree();
    }

    @GetMapping("/bizPath")
    public List<CatalogNode> bizPath(String bizId, String bizType) {
        return catalogService.getBizPath(bizId, bizType);
    }

    @GetMapping("/bizNodes")
    public List<Long> bizNodes(String bizId, String bizType) {
        return catalogService.getNodeIds(bizId, bizType);
    }

    @GetMapping("/nodeBiz")
    public List<String> nodeBiz(Long nodeId, String bizType) {
        return catalogService.getBizIdsByNodeTree(nodeId, bizType);
    }

    @GetMapping("/bizTree")
    public List<CatalogNode> bizTree(String bizId, String bizType) {
        return catalogService.getBizTree(bizId, bizType);
    }

    @GetMapping("/subtree")
    public List<CatalogNode> subtree(Long nodeId) {
        return catalogService.getSubtree(nodeId);
    }
}
