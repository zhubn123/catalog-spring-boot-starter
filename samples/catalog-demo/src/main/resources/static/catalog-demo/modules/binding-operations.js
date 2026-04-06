import { extractErrorMessage } from "../api.js";

export function createBindingActions(context) {
    const {
        api,
        ElMessage,
        selectedNode,
        bindForm,
        batchBindForm,
        loadTree,
        loadDirectBindings,
        bindingListBizType
    } = context;

    const bindBiz = async () => {
        if (!selectedNode.value) {
            ElMessage.warning("请先从左侧目录树选择一个节点");
            return;
        }
        if (!selectedNode.value.bindable) {
            ElMessage.warning("当前节点不是叶子节点，不能直接绑定业务对象");
            return;
        }

        const bizId = bindForm.bizId.trim();
        if (!bizId || !bindForm.bizType) {
            ElMessage.warning("请填写完整的业务绑定信息");
            return;
        }

        try {
            await api.postJson("/catalog/bind", {
                nodeId: selectedNode.value.id,
                bizId,
                bizType: bindForm.bizType
            });
            bindingListBizType.value = bindForm.bizType;
            bindForm.bizId = "";
            ElMessage.success("业务绑定成功");
            await loadTree();
        } catch (error) {
            ElMessage.error("业务绑定失败: " + extractErrorMessage(error));
        }
    };

    const batchBind = async () => {
        const nodeIds = batchBindForm.nodeIds
            .split(",")
            .map((item) => Number(item.trim()))
            .filter((item) => Number.isFinite(item) && item > 0);
        const bizIds = batchBindForm.bizIds
            .split(",")
            .map((item) => item.trim())
            .filter(Boolean);

        if (!batchBindForm.bizType || nodeIds.length === 0 || bizIds.length === 0) {
            ElMessage.warning("请填写完整的批量绑定信息");
            return;
        }
        if (nodeIds.length !== bizIds.length) {
            ElMessage.warning("节点 ID 和业务 ID 的数量必须一一对应");
            return;
        }

        try {
            await api.postJson("/catalog/bind/pairs", {
                nodeIds,
                bizIds,
                bizType: batchBindForm.bizType
            });
            ElMessage.success("批量绑定成功");
            await loadTree();
        } catch (error) {
            ElMessage.error("批量绑定失败: " + extractErrorMessage(error));
        }
    };

    const quickUnbind = async (binding) => {
        if (!selectedNode.value) {
            return;
        }

        try {
            await api.postJson("/catalog/unbind", {
                nodeId: selectedNode.value.id,
                bizId: binding.bizId,
                bizType: binding.bizType
            });
            ElMessage.success("绑定已解除");
            await loadDirectBindings(selectedNode.value.id);
        } catch (error) {
            ElMessage.error("解除绑定失败: " + extractErrorMessage(error));
        }
    };

    return {
        bindBiz,
        batchBind,
        quickUnbind
    };
}
