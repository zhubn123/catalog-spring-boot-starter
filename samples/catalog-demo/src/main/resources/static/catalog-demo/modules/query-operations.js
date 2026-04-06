export function createQueryActions(context) {
    const {
        api,
        ElMessage,
        queryBizPathForm,
        bizPathResult,
        queryBizNodesForm,
        bizNodesResult,
        queryNodeBizForm,
        nodeBizResult,
        queryBizTreeForm,
        bizTreeResult,
        querySubtreeForm,
        subtreeResult
    } = context;

    const queryBizPath = async () => {
        if (!queryBizPathForm.bizId || !queryBizPathForm.bizType) {
            ElMessage.warning("请填写完整信息");
            return;
        }
        try {
            const result = await api.get(`/catalog/bizPath?bizId=${queryBizPathForm.bizId}&bizType=${queryBizPathForm.bizType}`);
            bizPathResult.value = result;
            if (result.length === 0) {
                ElMessage.info("未找到路径");
            }
        } catch (error) {
            ElMessage.error("查询失败: " + (error.response?.data?.message || error.message));
        }
    };

    const queryBizNodes = async () => {
        if (!queryBizNodesForm.bizId || !queryBizNodesForm.bizType) {
            ElMessage.warning("请填写完整信息");
            return;
        }
        try {
            const result = await api.get(`/catalog/bizNodes?bizId=${queryBizNodesForm.bizId}&bizType=${queryBizNodesForm.bizType}`);
            bizNodesResult.value = result;
            if (result.length === 0) {
                ElMessage.info("未找到绑定的节点");
            }
        } catch (error) {
            ElMessage.error("查询失败: " + (error.response?.data?.message || error.message));
        }
    };

    const queryNodeBiz = async () => {
        if (!queryNodeBizForm.nodeId || !queryNodeBizForm.bizType) {
            ElMessage.warning("请填写完整信息");
            return;
        }
        try {
            const result = await api.get(`/catalog/nodeBiz?nodeId=${queryNodeBizForm.nodeId}&bizType=${queryNodeBizForm.bizType}`);
            nodeBizResult.value = result;
            if (result.length === 0) {
                ElMessage.info("该子树下暂无业务绑定");
            }
        } catch (error) {
            ElMessage.error("查询失败: " + (error.response?.data?.message || error.message));
        }
    };

    const queryBizTree = async () => {
        if (!queryBizTreeForm.bizId || !queryBizTreeForm.bizType) {
            ElMessage.warning("请填写完整信息");
            return;
        }
        try {
            const result = await api.get(`/catalog/bizTreeNodes?bizId=${queryBizTreeForm.bizId}&bizType=${queryBizTreeForm.bizType}`);
            bizTreeResult.value = result;
            if (result.length === 0) {
                ElMessage.info("未找到相关节点");
            } else {
                ElMessage.success(`找到 ${result.length} 个相关节点`);
            }
        } catch (error) {
            ElMessage.error("查询失败: " + (error.response?.data?.message || error.message));
        }
    };

    const querySubtree = async () => {
        if (!querySubtreeForm.nodeId) {
            ElMessage.warning("请输入节点ID");
            return;
        }
        try {
            const result = await api.get(`/catalog/subtreeNodes?nodeId=${querySubtreeForm.nodeId}`);
            subtreeResult.value = result;
            if (result.length === 0) {
                ElMessage.info("未找到子树节点");
            } else {
                ElMessage.success(`子树包含 ${result.length} 个节点`);
            }
        } catch (error) {
            ElMessage.error("查询失败: " + (error.response?.data?.message || error.message));
        }
    };

    return {
        queryBizPath,
        queryBizNodes,
        queryNodeBiz,
        queryBizTree,
        querySubtree
    };
}
