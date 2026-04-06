package com.example.demo.contract;

import com.example.demo.common.Result;
import com.example.demo.contract.entity.ContractBo;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合同目录演示接口。
 *
 * <p>负责创建合同目录及其交付物子目录，并返回生成后的合同节点 ID。</p>
 */
@RestController
@RequestMapping("/contract")
public class ContractController {

    private final ContractCatalogService contractCatalogService;

    public ContractController(ContractCatalogService contractCatalogService) {
        this.contractCatalogService = contractCatalogService;
    }

    /**
     * 创建合同目录。
     */
    @PutMapping
    public Result<?> createContractCatalog(@RequestBody ContractBo contractBo) {
        Long contractNodeId = contractCatalogService.initContractCatalog(contractBo);
        return Result.success(contractNodeId);
    }
}