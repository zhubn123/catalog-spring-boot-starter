import { extractErrorMessage, unwrapResultData } from "../api.js";

export function createBusinessActions(context) {
    const {
        api,
        ElMessage,
        selectedNode,
        contractForm,
        contractNodeId,
        attachForm,
        loadTree
    } = context;

    const addDeliveryItem = () => {
        contractForm.items.push({ deliveryId: "", deliveryType: "" });
    };

    const removeDeliveryItem = (index) => {
        contractForm.items.splice(index, 1);
    };

    const createContractCatalog = async () => {
        const contractId = contractForm.contractId.trim();
        const contractName = contractForm.contractName.trim();
        if (!contractId || !contractName) {
            ElMessage.warning("请先填写合同 ID 和合同名称");
            return;
        }

        try {
            // 业务示例只负责演示“合同目录 + 交付物叶子节点”的典型链路。
            const result = await api.put("/contract", {
                contract: {
                    contractId,
                    contractName
                },
                items: contractForm.items
                    .map((item) => ({
                        deliveryId: item.deliveryId.trim(),
                        deliveryType: item.deliveryType.trim()
                    }))
                    .filter((item) => item.deliveryId && item.deliveryType)
            });
            const createdNodeId = unwrapResultData(result);
            contractNodeId.value = createdNodeId;
            attachForm.contractNodeId = String(createdNodeId ?? "");
            ElMessage.success(`合同目录创建成功，节点 ID：${createdNodeId}`);
            await loadTree({ preserveSelection: false });
        } catch (error) {
            ElMessage.error("创建合同目录失败: " + extractErrorMessage(error));
        }
    };

    const useSelectedNodeAsProjectTarget = () => {
        if (!selectedNode.value) {
            ElMessage.warning("请先在左侧选择一个项目目录节点");
            return;
        }
        attachForm.projectNodeId = String(selectedNode.value.id);
    };

    const attachContractToProject = async () => {
        if (!attachForm.projectNodeId || !attachForm.contractNodeId) {
            ElMessage.warning("请先填写项目节点 ID 和合同节点 ID");
            return;
        }

        try {
            await api.postJson("/project/contracts/attach", {
                projectNodeId: Number(attachForm.projectNodeId),
                contractNodeId: Number(attachForm.contractNodeId)
            });
            ElMessage.success("合同目录挂载成功");
            await loadTree({ preserveSelection: false });
        } catch (error) {
            ElMessage.error("挂载合同目录失败: " + extractErrorMessage(error));
        }
    };

    return {
        addDeliveryItem,
        removeDeliveryItem,
        createContractCatalog,
        useSelectedNodeAsProjectTarget,
        attachContractToProject
    };
}
