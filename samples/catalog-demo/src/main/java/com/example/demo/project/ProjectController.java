package com.example.demo.project;

import com.example.demo.common.Result;
import com.example.demo.contract.ContractCatalogService;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目目录演示接口。
 *
 * <p>sample 中项目侧只保留“挂载合同目录”这一项职责，合同目录创建统一放到
 * {@code /contract} 侧处理，避免两个入口都承担创建能力。</p>
 */
@RestController
@RequestMapping("/project")
public class ProjectController {

    private final ContractCatalogService contractCatalogService;

    public ProjectController(ContractCatalogService contractCatalogService) {
        this.contractCatalogService = contractCatalogService;
    }

    /**
     * 将已创建好的合同目录挂载到项目目录下。
     *
     * <p>推荐使用 {@code /project/contracts/attach}，旧的 {@code /project/bindContract}
     * 作为兼容别名保留。</p>
     */
    @PostMapping({"/contracts/attach", "/bindContract"})
    public Result<?> attachContractToProject(@RequestBody AttachContractRequest request) {
        contractCatalogService.bindContractToProject(request.getProjectNodeId(), request.getContractNodeId());
        return Result.success();
    }

    /**
     * 合同目录挂载请求。
     */
    @Data
    public static class AttachContractRequest {
        private Long projectNodeId;
        private Long contractNodeId;
    }
}