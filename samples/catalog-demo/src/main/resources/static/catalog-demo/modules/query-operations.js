import { extractErrorMessage } from "../api.js";
import { normalizeTree } from "../tree-utils.js";

export function createQueryActions(context) {
    const {
        api,
        ElMessage,
        selectedNode,
        queryBizPathForm,
        bizPathResult,
        queryBizNodesForm,
        bizNodesResult,
        queryNodeBizForm,
        nodeBizResult,
        queryBizTreeForm,
        querySubtreeForm,
        queryTreeData,
        queryTreeTitle,
        queryTreeSummary,
        clearQueryTree
    } = context;

    const queryBizPath = async () => {
        if (!queryBizPathForm.bizId || !queryBizPathForm.bizType) {
            ElMessage.warning("请填写完整的业务路径查询条件");
            return;
        }
        try {
            const result = await api.get(
                `/catalog/bizPath?bizId=${queryBizPathForm.bizId}&bizType=${queryBizPathForm.bizType}`
            );
            bizPathResult.value = result;
            if (result.length === 0) {
                ElMessage.info("未找到该业务对象的目录路径");
            }
        } catch (error) {
            ElMessage.error("查询业务路径失败: " + extractErrorMessage(error));
        }
    };

    const queryBizNodes = async () => {
        if (!queryBizNodesForm.bizId || !queryBizNodesForm.bizType) {
            ElMessage.warning("请填写完整的业务节点查询条件");
            return;
        }
        try {
            const result = await api.get(
                `/catalog/bizNodes?bizId=${queryBizNodesForm.bizId}&bizType=${queryBizNodesForm.bizType}`
            );
            bizNodesResult.value = result;
            if (result.length === 0) {
                ElMessage.info("当前业务对象没有绑定目录节点");
            }
        } catch (error) {
            ElMessage.error("查询业务节点失败: " + extractErrorMessage(error));
        }
    };

    const queryNodeBiz = async () => {
        const effectiveNodeId = queryNodeBizForm.nodeId || selectedNode.value?.id || "";
        if (!effectiveNodeId || !queryNodeBizForm.bizType) {
            ElMessage.warning("请填写节点 ID 和业务类型，或先在左侧选中节点");
            return;
        }

        try {
            const result = await api.get(
                `/catalog/nodeBiz?nodeId=${effectiveNodeId}&bizType=${queryNodeBizForm.bizType}`
            );
            nodeBizResult.value = result;
            queryNodeBizForm.nodeId = String(effectiveNodeId);
            if (result.length === 0) {
                ElMessage.info("该节点子树下暂无此类型业务绑定");
            }
        } catch (error) {
            ElMessage.error("查询子树业务失败: " + extractErrorMessage(error));
        }
    };

    const queryBizTree = async () => {
        if (!queryBizTreeForm.bizId || !queryBizTreeForm.bizType) {
            ElMessage.warning("请填写完整的业务局部树查询条件");
            return;
        }

        try {
            // 直接使用后端返回的嵌套树，避免前端再拿扁平列表重新组装。
            const result = normalizeTree(await api.get(
                `/catalog/bizTree?bizId=${queryBizTreeForm.bizId}&bizType=${queryBizTreeForm.bizType}`
            ));
            queryTreeData.value = result;
            queryTreeTitle.value = "业务局部树";
            queryTreeSummary.value = result.length === 0
                ? "当前业务对象没有可展示的目录树"
                : "展示绑定节点及其祖先节点的树形结构";
            if (result.length === 0) {
                ElMessage.info("未找到该业务对象对应的局部树");
            }
        } catch (error) {
            clearQueryTree();
            ElMessage.error("查询业务局部树失败: " + extractErrorMessage(error));
        }
    };

    const querySubtree = async () => {
        const effectiveNodeId = querySubtreeForm.nodeId || selectedNode.value?.id || "";
        if (!effectiveNodeId) {
            ElMessage.warning("请填写节点 ID，或先从左侧选中节点");
            return;
        }

        try {
            // 子树预览优先复用当前选中节点，减少手填 nodeId 的负担。
            const result = normalizeTree(await api.get(`/catalog/subtree?nodeId=${effectiveNodeId}`));
            queryTreeData.value = result;
            queryTreeTitle.value = "节点子树";
            queryTreeSummary.value = result.length === 0
                ? "当前节点没有子树结果"
                : "展示当前节点及其全部后代节点";
            querySubtreeForm.nodeId = String(effectiveNodeId);
            if (result.length === 0) {
                ElMessage.info("未找到当前节点的子树");
            }
        } catch (error) {
            clearQueryTree();
            ElMessage.error("查询节点子树失败: " + extractErrorMessage(error));
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
