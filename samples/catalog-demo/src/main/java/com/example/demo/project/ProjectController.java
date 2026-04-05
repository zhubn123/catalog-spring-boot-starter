package com.example.demo.project;

import com.example.demo.common.Result;
import com.example.demo.contract.ContractCatalogService;
import com.example.demo.contract.entity.ContractBo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 项目目录控制器。
 *
 * <p>用于兼容历史接口，并提供合同目录挂载到项目目录下的能力。</p>
 *
 * @author zhubn
 * @date 2026/4/1
 * @see ContractCatalogService 合同目录编排服务
 */
@RestController
@RequestMapping("/project")
public class ProjectController {

    /**
     * 合同目录编排服务。
     */
    @Autowired
    private ContractCatalogService contractCatalogService;

    /**
     * 创建合同目录的兼容端点。
     *
     * @param contractBo 合同业务对象
     * @return 操作结果，data 字段为合同节点 ID
     */
    // @Operation(summary = "创建合同目录", description = "兼容端点，功能与 PUT /contract 相同")
    @PutMapping
    public Result<?> bindContract(@RequestBody ContractBo contractBo) {
        Long contractNodeId = contractCatalogService.initContractCatalog(contractBo);
        return Result.success(contractNodeId);
    }

    /**
     * 将合同目录挂载到项目目录下。
     *
     * @param projectNodeId 项目节点 ID
     * @param contractNodeId 合同节点 ID
     * @return 操作结果
     */
    // @Operation(summary = "挂载合同到项目", description = "将合同文件夹移动到项目文件夹下")
    @PostMapping("/bindContract")
    public Result<?> bindContractToProject(Long projectNodeId, Long contractNodeId) {
        contractCatalogService.bindContractToProject(projectNodeId, contractNodeId);
        return Result.success();
    }
}
