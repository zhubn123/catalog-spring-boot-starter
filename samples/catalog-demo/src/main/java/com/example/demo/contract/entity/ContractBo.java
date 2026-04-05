package com.example.demo.contract.entity;

import com.example.demo.contract.ContractCatalogService;
import lombok.Data;

import java.util.List;

/**
 * 合同目录创建请求对象。
 *
 * <p>包含合同基本信息和交付物列表，用于初始化合同目录。</p>
 *
 * @author zhubn
 * @date 2026/4/1
 * @see ContractCatalogService 合同目录编排服务
 * @see com.example.demo.contract.entity.Contract 合同实体
 * @see com.example.demo.contract.entity.DeliveryItem 交付物实体
 */
@Data
public class ContractBo {

    /**
     * 合同基本信息。
     * 合同名称会作为目录节点名称使用。
     */
    private Contract contract;

    /**
     * 交付物列表。
     * 每个交付物会在合同目录下创建对应子节点。
     */
    private List<DeliveryItem> items;
}
