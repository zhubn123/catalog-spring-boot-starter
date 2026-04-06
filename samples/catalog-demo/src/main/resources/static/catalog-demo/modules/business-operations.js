export function createBusinessActions(context) {
    const {
        api,
        ElMessage,
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
        if (!contractForm.contractId || !contractForm.contractName) {
            ElMessage.warning("请填写合同ID和名称");
            return;
        }
        try {
            const payload = {
                contract: {
                    contractId: contractForm.contractId,
                    contractName: contractForm.contractName
                },
                items: contractForm.items.filter((item) => item.deliveryId && item.deliveryType)
            };
            const result = await api.put("/contract", payload);
            contractNodeId.value = result.data;
            ElMessage.success(`创建成功，合同节点ID: ${result.data}`);
            await loadTree();
        } catch (error) {
            ElMessage.error("创建失败: " + (error.response?.data?.message || error.message));
        }
    };

    const attachContractToProject = async () => {
        if (!attachForm.projectNodeId || !attachForm.contractNodeId) {
            ElMessage.warning("请填写项目节点ID和合同节点ID");
            return;
        }
        try {
            await api.postJson("/project/contracts/attach", {
                projectNodeId: attachForm.projectNodeId,
                contractNodeId: attachForm.contractNodeId
            });
            ElMessage.success("挂载成功");
            await loadTree();
        } catch (error) {
            ElMessage.error("挂载失败: " + (error.response?.data?.message || error.message));
        }
    };

    return {
        addDeliveryItem,
        removeDeliveryItem,
        createContractCatalog,
        attachContractToProject
    };
}