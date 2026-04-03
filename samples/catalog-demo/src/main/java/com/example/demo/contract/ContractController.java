package com.example.demo.contract;

import com.example.demo.common.Result;
import com.example.demo.contract.entity.ContractBo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合同目录REST API控制器
 * 
 * <p>该控制器提供合同目录创建的HTTP入口点，是合同业务场景的API层。
 * 主要用于初始化合同目录结构。</p>
 * 
 * <h2>API列表</h2>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>功能</th></tr>
 *   <tr><td>PUT</td><td>/contract</td><td>创建合同目录</td></tr>
 * </table>
 * 
 * <h2>典型使用流程</h2>
 * <pre>
 * 1. 创建合同目录
 *    PUT /contract
 *    Body: {"contract": {...}, "items": [...]}
 *    返回: 合同节点ID
 * 
 * 2. 创建项目目录（使用通用API）
 *    POST /catalog/node?parentId=0&name=项目A
 *    返回: 项目节点ID
 * 
 * 3. 挂载合同到项目（使用项目API）
 *    POST /project/bindContract?projectNodeId=1&contractNodeId=2
 * </pre>
 * 
 * <h2>设计说明</h2>
 * <p>合同控制器与项目控制器分离的原因：</p>
 * <ul>
 *   <li><b>职责分离</b>：合同和项目是不同的业务实体</li>
 *   <li><b>独立创建</b>：合同可能在项目之前创建</li>
 *   <li><b>灵活挂载</b>：一个合同可能属于多个项目（复制挂载）</li>
 * </ul>
 * 
 * @author zhubn
 * @date 2026/4/1
 * @see ContractCatalogService 合同目录编排服务
 * @see ContractBo 合同业务对象
 */
@RestController
@RequestMapping("/contract")
public class ContractController {

    /**
     * 合同目录编排服务
     */
    @Autowired
    private ContractCatalogService contractCatalogService;

    /**
     * 创建合同目录
     * 
     * <p>根据合同信息和交付物列表创建完整的目录结构。
     * 返回合同节点ID，可用于后续挂载到项目下。</p>
     * 
     * <h3>请求示例：</h3>
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
     * <h3>返回示例：</h3>
     * <pre>
     * {
     *   "code": 200,
     *   "message": "success",
     *   "data": 123  // 合同节点ID
     * }
     * </pre>
     * 
     * <h3>创建的目录结构：</h3>
     * <pre>
     * 根目录
     * └── 软件开发合同 (返回的节点ID)
     *     ├── 设计文档 (绑定 DEL-001)
     *     ├── 源代码 (绑定 DEL-002)
     *     └── 测试报告 (绑定 DEL-003)
     * </pre>
     * 
     * @param contractBo 合同业务对象
     * @return 操作结果，data 字段为合同节点ID
     */
    // @Operation(summary = "创建合同目录", description = "创建合同文件夹和交付物子文件夹，返回合同节点ID")
    @PutMapping
    public Result<?> createContractCatalog(@RequestBody ContractBo contractBo) {
        Long contractNodeId = contractCatalogService.initContractCatalog(contractBo);
        return Result.success(contractNodeId);
    }
}
