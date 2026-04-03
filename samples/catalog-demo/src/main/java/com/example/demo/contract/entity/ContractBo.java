package com.example.demo.contract.entity;

import com.example.demo.contract.ContractCatalogService;
import lombok.Data;

import java.util.List;

/**
 * 合同业务对象
 * 
 * <p>该类是合同目录创建请求的数据传输对象（DTO），
 * 封装了合同基本信息和交付物列表。</p>
 * 
 * <h2>使用场景</h2>
 * <p>当需要创建合同目录时，前端提交该对象，包含：</p>
 * <ul>
 *   <li>合同基本信息（ID、名称）</li>
 *   <li>交付物列表（每个交付物的ID和类型）</li>
 * </ul>
 * 
 * <h2>请求示例</h2>
 * <pre>
 * PUT /contract
 * Content-Type: application/json
 * 
 * {
 *   "contract": {
 *     "contractId": "CONTRACT-001",
 *     "contractName": "软件开发合同"
 *   },
 *   "items": [
 *     {"deliveryId": "DEL-001", "deliveryType": "设计文档"},
 *     {"deliveryId": "DEL-002", "deliveryType": "源代码"},
 *     {"deliveryId": "DEL-003", "deliveryType": "测试报告"}
 *   ]
 * }
 * </pre>
 * 
 * <h2>处理流程</h2>
 * <ol>
 *   <li>创建合同文件夹（使用 contractName 作为文件夹名）</li>
 *   <li>为每个交付物创建子文件夹（使用 deliveryType 作为文件夹名）</li>
 *   <li>将 deliveryId 绑定到对应的叶子节点</li>
 *   <li>返回合同节点ID，供后续挂载到项目下使用</li>
 * </ol>
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
     * 合同基本信息
     * 
     * <p>包含合同ID和合同名称。合同名称会作为目录树的文件夹名称。</p>
     */
    private Contract contract;

    /**
     * 交付物列表
     * 
     * <p>每个交付物会在合同文件夹下创建一个子文件夹，
     * 并将 deliveryId 绑定到该子文件夹节点。</p>
     * 
     * <p>交付物类型（deliveryType）会作为子文件夹的名称。</p>
     */
    private List<DeliveryItem> items;
}
