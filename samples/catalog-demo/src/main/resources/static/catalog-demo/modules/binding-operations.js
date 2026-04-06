export function createBindingActions(context) {
    const {
        api,
        ElMessage,
        bindForm,
        batchBindForm,
        unbindForm,
        selectedNode,
        loadNodeBindings
    } = context;

    const bindBiz = async () => {
        if (!bindForm.nodeId || !bindForm.bizId || !bindForm.bizType) {
            ElMessage.warning("请填写完整信息");
            return;
        }
        try {
            await api.postJson("/catalog/bind", {
                nodeId: bindForm.nodeId,
                bizId: bindForm.bizId,
                bizType: bindForm.bizType
            });
            ElMessage.success("绑定成功");
            if (selectedNode.value) {
                await loadNodeBindings(selectedNode.value.id);
            }
        } catch (error) {
            ElMessage.error("绑定失败: " + (error.response?.data?.message || error.message));
        }
    };

    const batchBind = async () => {
        if (!batchBindForm.nodeIds || !batchBindForm.bizIds || !batchBindForm.bizType) {
            ElMessage.warning("请填写完整信息");
            return;
        }
        try {
            const nodeIds = batchBindForm.nodeIds.split(",").map((item) => item.trim()).filter(Boolean);
            const bizIds = batchBindForm.bizIds.split(",").map((item) => item.trim()).filter(Boolean);
            if (nodeIds.length !== bizIds.length) {
                ElMessage.warning("节点ID和业务ID数量必须一致");
                return;
            }
            await api.postJson("/catalog/bind/pairs", {
                nodeIds: nodeIds.map((item) => Number(item)),
                bizIds,
                bizType: batchBindForm.bizType
            });
            ElMessage.success("批量绑定成功");
        } catch (error) {
            ElMessage.error("批量绑定失败: " + (error.response?.data?.message || error.message));
        }
    };

    const unbindBiz = async () => {
        if (!unbindForm.nodeId || !unbindForm.bizId || !unbindForm.bizType) {
            ElMessage.warning("请填写完整信息");
            return;
        }
        try {
            await api.postJson("/catalog/unbind", {
                nodeId: unbindForm.nodeId,
                bizId: unbindForm.bizId,
                bizType: unbindForm.bizType
            });
            ElMessage.success("解除绑定成功");
        } catch (error) {
            ElMessage.error("解除绑定失败: " + (error.response?.data?.message || error.message));
        }
    };

    const quickUnbind = async (binding) => {
        try {
            await api.postJson("/catalog/unbind", {
                nodeId: selectedNode.value.id,
                bizId: binding.bizId,
                bizType: binding.bizType
            });
            ElMessage.success("解除绑定成功");
            await loadNodeBindings(selectedNode.value.id);
        } catch (error) {
            ElMessage.error("解除绑定失败: " + (error.response?.data?.message || error.message));
        }
    };

    return {
        bindBiz,
        batchBind,
        unbindBiz,
        quickUnbind
    };
}
