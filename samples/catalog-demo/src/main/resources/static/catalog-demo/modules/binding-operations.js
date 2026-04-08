import { extractErrorMessage } from "../api.js";

function splitBizIds(text) {
    return text
        .split(/[\n,，]+/)
        .map((item) => item.trim())
        .filter(Boolean);
}

export function createBindingActions(context) {
    const {
        api,
        ElMessage,
        selectedNode,
        bindForm,
        bindManyForm,
        directBindings,
        bindingListBizType,
        loadTree,
        loadDirectBindings
    } = context;

    const ensureBindableNode = () => {
        if (!selectedNode.value) {
            ElMessage.warning("请先从左侧目录树选择一个节点");
            return false;
        }
        if (!selectedNode.value.bindable) {
            ElMessage.warning("当前节点不是可绑定业务对象的叶子节点");
            return false;
        }
        return true;
    };

    const bindBiz = async () => {
        if (!ensureBindableNode()) {
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

    const bindManyBizToCurrentNode = async () => {
        if (!ensureBindableNode()) {
            return;
        }
        const bizIds = splitBizIds(bindManyForm.bizIdsText || "");
        if (bizIds.length === 0 || !bindForm.bizType) {
            ElMessage.warning("请填写要追加绑定的业务 ID 和业务类型");
            return;
        }

        const failures = [];
        let successCount = 0;
        for (const bizId of bizIds) {
            try {
                await api.postJson("/catalog/bind", {
                    nodeId: selectedNode.value.id,
                    bizId,
                    bizType: bindForm.bizType
                });
                successCount += 1;
            } catch (error) {
                failures.push(`${bizId}: ${extractErrorMessage(error)}`);
            }
        }

        bindingListBizType.value = bindForm.bizType;
        bindManyForm.bizIdsText = "";
        await loadTree();
        if (successCount > 0 && failures.length === 0) {
            ElMessage.success(`已追加绑定 ${successCount} 个业务对象`);
            return;
        }
        if (successCount > 0) {
            ElMessage.warning(`成功 ${successCount} 个，失败 ${failures.length} 个`);
            console.warn("bindManyBizToCurrentNode failures", failures);
            return;
        }
        ElMessage.error("批量追加绑定失败: " + failures.join("; "));
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
            const index = directBindings.value.findIndex((item) => item.bizId === binding.bizId && item.bizType === binding.bizType);
            if (index >= 0) {
                directBindings.value.splice(index, 1);
            }
            ElMessage.success("绑定已解除");
            await loadTree();
            await loadDirectBindings(selectedNode.value.id);
        } catch (error) {
            ElMessage.error("解除绑定失败: " + extractErrorMessage(error));
        }
    };

    return {
        bindBiz,
        bindManyBizToCurrentNode,
        quickUnbind
    };
}