package com.example.demo.contract;

import com.example.demo.common.Result;
import com.example.demo.contract.entity.ContractBo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合同目录控制器。
 *
 * <p>提供创建合同目录的接口，返回生成后的合同目录节点 ID。</p>
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
     * 合同目录编排服务。
     */
    @Autowired
    private ContractCatalogService contractCatalogService;

    /**
     * 创建合同目录。
     *
     * @param contractBo 合同业务对象
     * @return 操作结果，data 字段为合同节点 ID
     */
    // @Operation(summary = "创建合同目录", description = "创建合同文件夹和交付物子文件夹，返回合同节点ID")
    @PutMapping
    public Result<?> createContractCatalog(@RequestBody ContractBo contractBo) {
        Long contractNodeId = contractCatalogService.initContractCatalog(contractBo);
        return Result.success(contractNodeId);
    }
}
