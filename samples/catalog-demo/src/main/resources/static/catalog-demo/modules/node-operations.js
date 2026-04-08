import { extractErrorMessage } from "../api.js";

function splitTextValues(text) {
    return text
        .split(/[\n,，]+/)
        .map((item) => item.trim())
        .filter(Boolean);
}

export function createNodeActions(context) {
    const {
        api,
        ElMessage,
        ElMessageBox,
        activeTab,
        selectedNode,
        addForm,
        updateForm,
        loadTree,
        clearSelection,
        syncFormsFromSelectedNode
    } = context;

    const addNode = async () => {
        const names = splitTextValues(addForm.name || "");
        if (names.length === 0) {
            ElMessage.warning("请先填写节点名称");
            return;
        }

        try {
            if (names.length === 1) {
                const payload = { name: names[0] };
                if (addForm.parentId) {
                    payload.parentId = Number(addForm.parentId);
                }
                await api.postJson("/catalog/node", payload);
                ElMessage.success("节点创建成功");
            } else {
                const payload = { names };
                if (addForm.parentId) {
                    payload.parentId = Number(addForm.parentId);
                }
                await api.postJson("/catalog/node/batch", payload);
                ElMessage.success(`已批量创建 ${names.length} 个节点`);
            }
            addForm.name = "";
            await loadTree();
        } catch (error) {
            ElMessage.error("节点创建失败: " + extractErrorMessage(error));
        }
    };

    const updateSelectedNode = async () => {
        if (!selectedNode.value) {
            ElMessage.warning("请先选择一个节点");
            return;
        }

        const payload = { nodeId: Number(updateForm.nodeId) };
        if (updateForm.name && updateForm.name.trim()) {
            payload.name = updateForm.name.trim();
        }
        if (updateForm.code && updateForm.code.trim()) {
            payload.code = updateForm.code.trim();
        }
        if (updateForm.sort !== "" && updateForm.sort != null) {
            payload.sort = Number(updateForm.sort);
        }

        if (Object.keys(payload).length === 1) {
            ElMessage.warning("请至少修改一个字段再保存");
            return;
        }

        try {
            await api.postJson("/catalog/node/update", payload);
            ElMessage.success("节点更新成功");
            await loadTree();
        } catch (error) {
            ElMessage.error("节点更新失败: " + extractErrorMessage(error));
        }
    };

    const handleDelete = async (node = selectedNode.value) => {
        if (!node) {
            ElMessage.warning("请先选择一个节点");
            return;
        }

        try {
            await ElMessageBox.confirm(`确定删除节点“${node.name}”及其子树吗？`, "删除确认", { type: "warning" });
            await api.postJson("/catalog/node/delete", { nodeId: node.id, recursive: true });
            ElMessage.success("节点删除成功");
            if (selectedNode.value && String(selectedNode.value.id) === String(node.id)) {
                clearSelection();
                await loadTree({ preserveSelection: false });
                return;
            }
            await loadTree();
        } catch (error) {
            if (error !== "cancel") {
                ElMessage.error("节点删除失败: " + extractErrorMessage(error));
            }
        }
    };

    const showAddRootDialog = () => {
        activeTab.value = "node";
        addForm.parentId = "";
    };

    const showAddChildDialog = (node) => {
        syncFormsFromSelectedNode(node);
        activeTab.value = "node";
        addForm.parentId = String(node.id);
    };

    const showEditDialog = (node) => {
        syncFormsFromSelectedNode(node);
        activeTab.value = "node";
    };

    return {
        addNode,
        updateSelectedNode,
        handleDelete,
        showAddRootDialog,
        showAddChildDialog,
        showEditDialog
    };
}