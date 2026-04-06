export function createNodeActions(context) {
    const {
        api,
        ElMessage,
        ElMessageBox,
        addForm,
        batchAddForm,
        moveForm,
        updateForm,
        activeTab,
        editDialogVisible,
        editForm,
        selectedNode,
        loadTree
    } = context;

    const addNode = async () => {
        if (!addForm.name) {
            ElMessage.warning("请输入节点名称");
            return;
        }
        try {
            const params = { name: addForm.name };
            if (addForm.parentId) {
                params.parentId = addForm.parentId;
            }
            await api.post("/catalog/node", params);
            ElMessage.success("添加成功");
            addForm.name = "";
            await loadTree();
        } catch (error) {
            ElMessage.error("添加失败: " + (error.response?.data?.message || error.message));
        }
    };

    const batchAddNodes = async () => {
        if (!batchAddForm.names) {
            ElMessage.warning("请输入节点名称");
            return;
        }
        try {
            const names = batchAddForm.names.split(",").map((item) => item.trim()).filter(Boolean);
            const params = { names: names.join(",") };
            if (batchAddForm.parentId) {
                params.parentId = batchAddForm.parentId;
            }
            await api.post("/catalog/node/batch", params);
            ElMessage.success(`批量添加 ${names.length} 个节点成功`);
            batchAddForm.names = "";
            await loadTree();
        } catch (error) {
            ElMessage.error("批量添加失败: " + (error.response?.data?.message || error.message));
        }
    };

    const moveNode = async () => {
        if (!moveForm.nodeId) {
            ElMessage.warning("请输入节点ID");
            return;
        }
        try {
            const params = { nodeId: moveForm.nodeId };
            if (moveForm.parentId) {
                params.parentId = moveForm.parentId;
            }
            if (moveForm.targetIndex) {
                params.targetIndex = moveForm.targetIndex;
            }
            await api.post("/catalog/move", params);
            ElMessage.success("移动成功");
            await loadTree();
        } catch (error) {
            ElMessage.error("移动失败: " + (error.response?.data?.message || error.message));
        }
    };

    const updateNode = async () => {
        if (!updateForm.nodeId) {
            ElMessage.warning("请输入节点ID");
            return;
        }
        try {
            const params = { nodeId: updateForm.nodeId };
            if (updateForm.name) {
                params.name = updateForm.name;
            }
            if (updateForm.code) {
                params.code = updateForm.code;
            }
            if (updateForm.sort !== "") {
                params.sort = updateForm.sort;
            }
            await api.post("/catalog/node/update", params);
            ElMessage.success("更新成功");
            await loadTree();
        } catch (error) {
            ElMessage.error("更新失败: " + (error.response?.data?.message || error.message));
        }
    };

    const handleDelete = async (node) => {
        try {
            await ElMessageBox.confirm(
                `确定删除节点 "${node.name}" 吗？\n如果是非叶子节点将递归删除子树。`,
                "删除确认",
                { type: "warning" }
            );
            await api.post("/catalog/node/delete", { nodeId: node.id, recursive: true });
            ElMessage.success("删除成功");
            selectedNode.value = null;
            await loadTree();
        } catch (error) {
            if (error !== "cancel") {
                ElMessage.error("删除失败: " + (error.response?.data?.message || error.message));
            }
        }
    };

    const showAddRootDialog = () => {
        addForm.parentId = "";
        activeTab.value = "node";
    };

    const showAddChildDialog = (node) => {
        addForm.parentId = node.id;
        activeTab.value = "node";
    };

    const showEditDialog = (node) => {
        editForm.id = node.id;
        editForm.name = node.name;
        editForm.code = node.code || "";
        editForm.sort = node.sort;
        editDialogVisible.value = true;
    };

    const saveEdit = async () => {
        try {
            await api.post("/catalog/node/update", {
                nodeId: editForm.id,
                name: editForm.name,
                code: editForm.code,
                sort: editForm.sort
            });
            ElMessage.success("保存成功");
            editDialogVisible.value = false;
            await loadTree();
        } catch (error) {
            ElMessage.error("保存失败: " + (error.response?.data?.message || error.message));
        }
    };

    return {
        addNode,
        batchAddNodes,
        moveNode,
        updateNode,
        handleDelete,
        showAddRootDialog,
        showAddChildDialog,
        showEditDialog,
        saveEdit
    };
}
