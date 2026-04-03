package com.example.demo.contract;

import com.berlin.catalog.service.CatalogService;
import com.example.demo.contract.entity.ContractBo;
import com.example.demo.contract.entity.DeliveryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

    /**
 * 合同目录编排服务
 * 
 * <p>该服务是合同业务场景下的目录编排层，封装了合同目录创建的标准流程。
 * 它组合使用 {@link CatalogService} 的通用能力，实现合同特定的业务逻辑。</p>
 * 
 * <h2>为什么需要这个服务？</h2>
 * <ul>
 *   <li><b>职责分离</b>：CatalogService 只提供通用树操作，不涉及具体业务</li>
 *   <li><b>流程编排</b>：合同目录创建有固定的步骤顺序，需要编排多个通用操作</li>
 *   <li><b>延迟挂载</b>：支持合同先创建，项目后创建，最后挂载的灵活流程</li>
 * </ul>
 * 
 * <h2>合同目录创建流程</h2>
 * <pre>
 * 阶段1：创建合同目录（项目未创建时）
 * ┌─────────────────────────────────────────────────────┐
 * │ 1. 创建合同文件夹（临时放在根目录下）                    │
 * │    └── 使用 contractName 作为文件夹名                  │
 * │                                                       │
 * │ 2. 绑定合同业务ID到合同节点                             │
 * │    └── 绑定关系：nodeId → contractId (bizType=contract)│
 * │                                                       │
 * │ 3. 为每个交付物创建子文件夹                             │
 * │    └── 使用 deliveryType 作为文件夹名                  │
 * │                                                       │
 * │ 4. 将交付物ID绑定到叶子节点                             │
 * │    └── 绑定关系：nodeId → deliveryId (bizType=deliver)│
 * │                                                       │
 * │ 5. 返回合同节点ID，供后续挂载使用                        │
 * └─────────────────────────────────────────────────────┘
 * 
 * 阶段2：挂载到项目（项目创建后）
 * ┌─────────────────────────────────────────────────────┐
 * │ 6. 创建项目文件夹，绑定项目业务ID                        │
 * │                                                       │
 * │ 7. 将合同文件夹移动到项目文件夹下                        │
 * │    └── 调用 bindContractToProject()                   │
 * └─────────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>目录结构示例</h2>
 * <pre>
 * 创建前（合同临时在根目录）：
 * 根目录
 * └── 软件开发合同 (contractNodeId) ← 绑定 CONTRACT-001
 *     ├── 设计文档 (绑定 DEL-001)
 *     ├── 源代码 (绑定 DEL-002)
 *     └── 测试报告 (绑定 DEL-003)
 * 
 * 挂载后（合同移入项目）：
 * 根目录
 * └── 项目A (projectNodeId) ← 绑定 PROJ-001
 *     └── 软件开发合同 (contractNodeId) ← 绑定 CONTRACT-001
 *         ├── 设计文档 (绑定 DEL-001)
 *         ├── 源代码 (绑定 DEL-002)
 *         └── 测试报告 (绑定 DEL-003)
 * 
 * catalog_rel 绑定关系：
 * | node_id | biz_id       | biz_type  |
 * |---------|--------------|-----------|
 * | 158     | PROJ-001     | project   |
 * | 154     | CONTRACT-001 | contract  |
 * | 155     | DEL-001      | deliver   |
 * | 156     | DEL-002      | deliver   |
 * | 157     | DEL-003      | deliver   |
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>
 * // 注入服务
 * {@literal @}Autowired
 * private ContractCatalogService contractCatalogService;
 * 
 * // 阶段1：创建合同目录
 * ContractBo contractBo = new ContractBo();
 * contractBo.setContract(new Contract("CONTRACT-001", "软件开发合同"));
 * contractBo.setItems(Arrays.asList(
 *     new DeliveryItem("DEL-001", "设计文档"),
 *     new DeliveryItem("DEL-002", "源代码")
 * ));
 * Long contractNodeId = contractCatalogService.initContractCatalog(contractBo);
 * 
 * // 阶段2：项目创建后，挂载合同到项目
 * Long projectNodeId = catalogService.addNode(0L, "项目A");
 * contractCatalogService.bindContractToProject(projectNodeId, contractNodeId);
 * </pre>
 * 
 * @author zhubn
 * @date 2026/4/1
 * @see CatalogService 通用目录服务
 * @see ContractBo 合同业务对象
 */
@Service
public class ContractCatalogService {

    /**
     * 合同文件夹的初始父节点ID
     * 
     * <p>使用 -1L 表示合同文件夹临时创建在根目录下，
     * 等待后续挂载到具体项目。这样设计的原因：</p>
     * <ul>
     *   <li>合同可能先于项目创建</li>
     *   <li>合同可能独立存在（不属于任何项目）</li>
     *   <li>避免创建临时节点再移动的复杂性</li>
     * </ul>
     */
    private static final Long INITIAL_CONTRACT_PARENT_ID = -1L;

    /**
     * 交付物业务类型常量
     * 
     * <p>绑定关系中的 bizType 字段值，用于区分不同类型的业务对象。</p>
     */
    private static final String BIZ_TYPE_DELIVER = "deliver";

    /**
     * 合同业务类型常量
     */
    private static final String BIZ_TYPE_CONTRACT = "contract";

    /**
     * 通用目录服务
     */
    @Autowired
    private CatalogService catalogService;

    /**
     * 初始化合同目录并返回合同节点ID
     * 
     * <p>创建合同文件夹和交付物子文件夹，并建立绑定关系。
     * 返回的合同节点ID可用于后续挂载到项目下。</p>
     * 
     * <h3>处理步骤：</h3>
     * <ol>
     *   <li>校验请求参数有效性</li>
     *   <li>创建合同文件夹（临时放在根目录）</li>
     *   <li>批量创建交付物子文件夹</li>
     *   <li>批量绑定交付物ID到叶子节点</li>
     *   <li>返回合同节点ID</li>
     * </ol>
     * 
     * <h3>注意事项：</h3>
     * <ul>
     *   <li>合同节点会绑定合同业务ID（bizType=contract）</li>
     *   <li>交付物叶子节点会绑定交付物ID（bizType=deliver）</li>
     *   <li>如果交付物列表为空，只创建合同文件夹并绑定合同ID</li>
     * </ul>
     * 
     * @param contractBo 合同业务对象，包含合同信息和交付物列表
     * @return 合同节点ID，用于后续挂载到项目
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Long initContractCatalog(ContractBo contractBo) {
        // ==================== 步骤1：参数校验 ====================
        
        // 校验请求体
        if (contractBo == null || contractBo.getContract() == null) {
            throw new IllegalArgumentException("contract payload must not be null");
        }

        // 提取并校验合同信息
        String contractId = trimToNull(contractBo.getContract().getContractId());
        String contractName = trimToNull(contractBo.getContract().getContractName());
        if (contractId == null) {
            throw new IllegalArgumentException("contractId must not be blank");
        }
        if (contractName == null) {
            throw new IllegalArgumentException("contractName must not be blank");
        }

        // ==================== 步骤2：创建合同文件夹 ====================
        
        // 创建合同文件夹，临时放在根目录下（parentId = -1）
        // 后续可以通过 bindContractToProject() 挂载到项目下
        Long contractNodeId = catalogService.addNode(INITIAL_CONTRACT_PARENT_ID, contractName);

        // ==================== 步骤2.1：绑定合同ID到合同节点 ====================
        
        // 将合同业务ID绑定到合同节点，便于后续通过合同ID查询目录路径
        catalogService.bind(contractNodeId, contractId, BIZ_TYPE_CONTRACT);

        // ==================== 步骤3：处理交付物列表 ====================
        
        List<DeliveryItem> items = contractBo.getItems();
        if (items == null || items.isEmpty()) {
            // 无交付物，直接返回合同节点ID
            return contractNodeId;
        }

        // 过滤有效的交付物项
        List<DeliveryItem> validItems = items.stream()
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.hasText(item.getDeliveryType()))
                .toList();
        if (validItems.isEmpty()) {
            return contractNodeId;
        }

        // ==================== 步骤4：批量创建交付物文件夹 ====================
        
        // 提取交付物类型作为文件夹名称
        String[] deliveryTypes = validItems.stream()
                .map(DeliveryItem::getDeliveryType)
                .toArray(String[]::new);
        
        // 批量创建交付物文件夹（作为合同文件夹的子节点）
        List<Long> deliveryNodeIds = catalogService.batchAddNode(contractNodeId, deliveryTypes);
        if (deliveryNodeIds.isEmpty()) {
            return contractNodeId;
        }

        // ==================== 步骤5：批量绑定交付物ID ====================
        
        // 提取交付物ID列表
        List<String> deliveryIds = validItems.stream()
                .map(DeliveryItem::getDeliveryId)
                .toList();
        
        // 批量绑定：deliveryNodeIds[i] 绑定 deliveryIds[i]
        catalogService.batchBindByBizIds(deliveryNodeIds, deliveryIds, BIZ_TYPE_DELIVER);
        
        return contractNodeId;
    }

    /**
     * 将合同文件夹挂载到项目文件夹下
     * 
     * <p>将预先创建的合同文件夹移动到指定项目文件夹下。
     * 这是"延迟挂载"模式的关键操作。</p>
     * 
     * <h3>使用场景：</h3>
     * <pre>
     * 1. 合同先创建（项目还未创建）
     *    - 调用 initContractCatalog() 创建合同目录
     *    - 保存返回的 contractNodeId
     * 
     * 2. 项目创建后
     *    - 创建项目文件夹，获得 projectNodeId
     *    - 调用此方法将合同挂载到项目下
     * </pre>
     * 
     * <h3>注意事项：</h3>
     * <ul>
     *   <li>项目节点必须已存在</li>
     *   <li>合同节点必须已存在</li>
     *   <li>移动操作会自动更新所有子节点的路径和层级</li>
     * </ul>
     * 
     * @param projectNodeId 项目节点ID
     * @param contractNodeId 合同节点ID
     * @throws IllegalArgumentException 当节点ID无效时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindContractToProject(Long projectNodeId, Long contractNodeId) {
        // 参数校验
        if (projectNodeId == null || projectNodeId <= 0) {
            throw new IllegalArgumentException("projectNodeId is invalid");
        }
        if (contractNodeId == null || contractNodeId <= 0) {
            throw new IllegalArgumentException("contractNodeId is invalid");
        }

        // 移动合同文件夹到项目文件夹下
        catalogService.moveNode(contractNodeId, projectNodeId);
    }

    /**
     * 字符串清理：去除首尾空白，空字符串转为 null
     * 
     * @param value 原始字符串
     * @return 清理后的字符串，空白返回 null
     */
    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
