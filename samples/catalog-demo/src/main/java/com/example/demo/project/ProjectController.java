package com.example.demo.project;

import com.example.demo.common.Result;
import com.example.demo.contract.ContractCatalogService;
import com.example.demo.contract.entity.ContractBo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 项目目录REST API控制器
 * 
 * <p>该控制器提供项目相关的目录操作HTTP入口点，主要处理"延迟挂载"场景：
 * 合同目录可以先创建，项目创建后再将合同挂载到项目下。</p>
 * 
 * <h2>API列表</h2>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>功能</th></tr>
 *   <tr><td>PUT</td><td>/project</td><td>创建合同目录（兼容端点）</td></tr>
 *   <tr><td>POST</td><td>/project/bindContract</td><td>挂载合同到项目</td></tr>
 * </table>
 * 
 * <h2>延迟挂载流程</h2>
 * <pre>
 * 场景：合同先于项目创建
 * 
 * 步骤1：创建合同目录（项目还未创建）
 *        PUT /project
 *        Body: {"contract": {...}, "items": [...]}
 *        返回: contractNodeId = 100
 * 
 * 步骤2：创建项目文件夹（使用通用API）
 *        POST /catalog/node?parentId=0&name=项目A
 *        返回: projectNodeId = 200
 * 
 * 步骤3：挂载合同到项目
 *        POST /project/bindContract?projectNodeId=200&contractNodeId=100
 * 
 * 最终目录结构：
 * 根目录
 * └── 项目A (200)
 *     └── 软件开发合同 (100)
 *         ├── 设计文档
 *         ├── 源代码
 *         └── 测试报告
 * </pre>
 * 
 * <h2>设计说明</h2>
 * <p>为什么在项目控制器中提供创建合同的兼容端点？</p>
 * <ul>
 *   <li><b>历史兼容</b>：保持旧版API的向后兼容</li>
 *   <li><b>简化调用</b>：某些场景下项目端点更便于前端调用</li>
 *   <li><b>统一入口</b>：项目相关的操作集中在一个控制器</li>
 * </ul>
 * 
 * @author zhubn
 * @date 2026/4/1
 * @see ContractCatalogService 合同目录编排服务
 */
@RestController
@RequestMapping("/project")
public class ProjectController {

    /**
     * 合同目录编排服务
     * 
     * <p>注入合同服务而非项目服务的原因：
     * 项目控制器主要负责协调合同目录的创建和挂载，
     * 核心业务逻辑在 ContractCatalogService 中实现。</p>
     */
    @Autowired
    private ContractCatalogService contractCatalogService;

    /**
     * 创建合同目录（兼容端点）
     * 
     * <p>这是 /contract 端点的兼容别名，功能完全相同。
     * 初始化合同目录并返回合同节点ID。</p>
     * 
     * <h3>请求示例：</h3>
     * <pre>
     * PUT /project
     * Content-Type: application/json
     * 
     * {
     *   "contract": {
     *     "contractId": "CONTRACT-001",
     *     "contractName": "软件开发合同"
     *   },
     *   "items": [
     *     {"deliveryId": "DEL-001", "deliveryType": "设计文档"}
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
     * @param contractBo 合同业务对象
     * @return 操作结果，data 字段为合同节点ID
     */
    // @Operation(summary = "创建合同目录", description = "兼容端点，功能与 PUT /contract 相同")
    @PutMapping
    public Result<?> bindContract(@RequestBody ContractBo contractBo) {
        Long contractNodeId = contractCatalogService.initContractCatalog(contractBo);
        return Result.success(contractNodeId);
    }

    /**
     * 挂载合同文件夹到项目文件夹下
     * 
     * <p>将预先创建的合同目录移动到指定项目目录下。
     * 这是"延迟挂载"模式的核心操作。</p>
     * 
     * <h3>使用场景：</h3>
     * <pre>
     * 场景：合同已创建，项目刚创建
     * 
     * 1. 已有合同节点：contractNodeId = 100
     * 2. 新建项目节点：projectNodeId = 200
     * 3. 调用此API挂载
     * </pre>
     * 
     * <h3>请求示例：</h3>
     * <pre>
     * POST /project/bindContract?projectNodeId=200&contractNodeId=100
     * </pre>
     * 
     * <h3>返回示例：</h3>
     * <pre>
     * {
     *   "code": 200,
     *   "message": "success"
     * }
     * </pre>
     * 
     * <h3>挂载前后对比：</h3>
     * <pre>
     * 挂载前：
     * 根目录
     * ├── 项目A (200)
     * └── 软件开发合同 (100)
     *     └── ...
     * 
     * 挂载后：
     * 根目录
     * └── 项目A (200)
     *     └── 软件开发合同 (100)
     *         └── ...
     * </pre>
     * 
     * @param projectNodeId 项目节点ID
     * @param contractNodeId 合同节点ID
     * @return 操作结果
     */
    // @Operation(summary = "挂载合同到项目", description = "将合同文件夹移动到项目文件夹下")
    @PostMapping("/bindContract")
    public Result<?> bindContractToProject(Long projectNodeId, Long contractNodeId) {
        contractCatalogService.bindContractToProject(projectNodeId, contractNodeId);
        return Result.success();
    }
}
