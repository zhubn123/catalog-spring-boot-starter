package com.example.demo.contract;

import com.example.demo.contract.entity.ContractBo;
import com.example.demo.contract.entity.DeliveryItem;
import io.github.zhubn123.catalog.service.CatalogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 合同目录编排服务。
 *
 * <p>负责创建合同目录、创建交付物子目录，以及在项目创建后将合同目录挂载到项目下。</p>
 */
@Service
public class ContractCatalogService {

    /**
     * 合同目录默认先创建在根目录下，挂载到项目属于后续动作。
     */
    private static final Long INITIAL_CONTRACT_PARENT_ID = -1L;

    private static final String BIZ_TYPE_DELIVER = "deliver";
    private static final String BIZ_TYPE_CONTRACT = "contract";

    private final CatalogService catalogService;

    public ContractCatalogService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * 初始化合同目录并返回合同节点 ID。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long initContractCatalog(ContractBo contractBo) {
        if (contractBo == null || contractBo.getContract() == null) {
            throw new IllegalArgumentException("contract payload must not be null");
        }

        String contractId = trimToNull(contractBo.getContract().getContractId());
        String contractName = trimToNull(contractBo.getContract().getContractName());
        if (contractId == null) {
            throw new IllegalArgumentException("contractId must not be blank");
        }
        if (contractName == null) {
            throw new IllegalArgumentException("contractName must not be blank");
        }

        Long contractNodeId = catalogService.addNode(INITIAL_CONTRACT_PARENT_ID, contractName);
        catalogService.bind(contractNodeId, contractId, BIZ_TYPE_CONTRACT);

        List<DeliveryItem> items = contractBo.getItems();
        if (items == null || items.isEmpty()) {
            return contractNodeId;
        }

        List<DeliveryItem> validItems = items.stream()
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.hasText(item.getDeliveryType()))
                .filter(item -> StringUtils.hasText(item.getDeliveryId()))
                .toList();
        if (validItems.isEmpty()) {
            return contractNodeId;
        }

        String[] deliveryTypes = validItems.stream()
                .map(DeliveryItem::getDeliveryType)
                .toArray(String[]::new);

        List<Long> deliveryNodeIds = catalogService.batchAddNode(contractNodeId, deliveryTypes);
        if (deliveryNodeIds.isEmpty()) {
            return contractNodeId;
        }

        List<String> deliveryIds = validItems.stream()
                .map(DeliveryItem::getDeliveryId)
                .toList();

        catalogService.batchBindByBizIds(deliveryNodeIds, deliveryIds, BIZ_TYPE_DELIVER);
        return contractNodeId;
    }

    /**
     * 将合同目录挂载到项目目录下。
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindContractToProject(Long projectNodeId, Long contractNodeId) {
        if (projectNodeId == null || projectNodeId <= 0) {
            throw new IllegalArgumentException("projectNodeId is invalid");
        }
        if (contractNodeId == null || contractNodeId <= 0) {
            throw new IllegalArgumentException("contractNodeId is invalid");
        }

        catalogService.moveNode(contractNodeId, projectNodeId);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}