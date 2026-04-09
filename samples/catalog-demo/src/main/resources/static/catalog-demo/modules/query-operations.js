import { extractErrorMessage } from "../api.js";
import { normalizeTree } from "../tree-utils.js";

function resolveParentId(rawParentId, selectedNode) {
    if (rawParentId !== null && rawParentId !== undefined && String(rawParentId).trim() !== "") {
        return Number(rawParentId);
    }
    if (selectedNode?.value?.id) {
        return Number(selectedNode.value.id);
    }
    return null;
}

export function createQueryActions(context) {
    const {
        api,
        ElMessage,
        selectedNode,
        bizPathResult,
        queryBizPathForm,
        childrenQueryForm,
        childrenItems,
        childrenMeta,
        queryBizTreeForm,
        querySubtreeForm,
        queryTreeData,
        queryTreeTitle,
        queryTreeSummary,
        repairForm,
        repairResult,
        clearQueryTree
    } = context;

    const queryBizPath = async () => {
        if (!queryBizPathForm.bizId || !queryBizPathForm.bizType) {
            ElMessage.warning("请填写完整的业务路径查询条件");
            return;
        }
        try {
            const result = await api.get(`/catalog/bizPath?bizId=${queryBizPathForm.bizId}&bizType=${queryBizPathForm.bizType}`);
            bizPathResult.value = result;
            if (result.length === 0) {
                ElMessage.info("未找到该业务对象的目录路径");
            }
        } catch (error) {
            ElMessage.error("查询业务路径失败: " + extractErrorMessage(error));
        }
    };

    const queryChildren = async () => {
        const parentId = resolveParentId(childrenQueryForm.parentId, selectedNode);
        if (!parentId || parentId <= 0) {
            ElMessage.warning("根节点请使用 childrenPage，children 仅支持正数 parentId");
            return;
        }
        try {
            const result = await api.get(`/catalog/children?parentId=${parentId}`);
            childrenItems.value = result;
            childrenMeta.value = { mode: "list", parentId };
            if (result.length === 0) {
                ElMessage.info("当前父节点下暂无直接子节点");
            }
        } catch (error) {
            ElMessage.error("查询直接子节点失败: " + extractErrorMessage(error));
        }
    };

    const queryChildrenPage = async () => {
        const resolvedParentId = resolveParentId(childrenQueryForm.parentId, selectedNode);
        const parentId = resolvedParentId == null ? 0 : resolvedParentId;
        const page = Number(childrenQueryForm.page || 1);
        const size = Number(childrenQueryForm.size || 20);
        try {
            const result = await api.get(`/catalog/childrenPage?parentId=${parentId}&page=${page}&size=${size}`);
            childrenItems.value = result.items || [];
            childrenMeta.value = {
                mode: "page",
                parentId,
                page: result.page,
                size: result.size,
                total: result.total,
                hasNext: result.hasNext
            };
            if ((result.items || []).length === 0) {
                ElMessage.info("当前分页结果为空");
            }
        } catch (error) {
            ElMessage.error("分页查询子节点失败: " + extractErrorMessage(error));
        }
    };

    const queryBizTree = async () => {
        if (!queryBizTreeForm.bizId || !queryBizTreeForm.bizType) {
            ElMessage.warning("请填写完整的业务局部树查询条件");
            return;
        }
        try {
            const result = normalizeTree(await api.get(`/catalog/bizTree?bizId=${queryBizTreeForm.bizId}&bizType=${queryBizTreeForm.bizType}`));
            queryTreeData.value = result;
            queryTreeTitle.value = "业务局部树";
            queryTreeSummary.value = result.length === 0 ? "当前业务对象没有可展示的局部树。" : "展示业务对象绑定节点及其祖先节点。";
            if (result.length === 0) {
                ElMessage.info("未找到该业务对象对应的局部树");
            }
        } catch (error) {
            clearQueryTree();
            ElMessage.error("查询业务局部树失败: " + extractErrorMessage(error));
        }
    };

    const querySubtree = async () => {
        const nodeId = querySubtreeForm.nodeId || selectedNode.value?.id || "";
        if (!nodeId) {
            ElMessage.warning("请填写节点 ID，或先从左侧选择节点");
            return;
        }
        try {
            const result = normalizeTree(await api.get(`/catalog/subtree?nodeId=${nodeId}`));
            queryTreeData.value = result;
            queryTreeTitle.value = "节点子树";
            queryTreeSummary.value = result.length === 0 ? "当前节点没有子树结果。" : "展示当前节点及其全部后代节点。";
            querySubtreeForm.nodeId = String(nodeId);
            if (result.length === 0) {
                ElMessage.info("未找到当前节点的子树");
            }
        } catch (error) {
            clearQueryTree();
            ElMessage.error("查询节点子树失败: " + extractErrorMessage(error));
        }
    };

    const repairSelectedParentSorts = async () => {
        const parentId = selectedNode.value?.parentId ?? 0;
        try {
            repairResult.value = await api.post("/catalog/admin/repairSort", { parentId });
            ElMessage.success("已完成当前父层排序修复");
        } catch (error) {
            ElMessage.error("修复排序失败: " + extractErrorMessage(error));
        }
    };

    const repairRootSorts = async () => {
        try {
            repairResult.value = await api.post("/catalog/admin/repairSort", { parentId: 0 });
            ElMessage.success("已完成根节点层排序修复");
        } catch (error) {
            ElMessage.error("修复根节点层排序失败: " + extractErrorMessage(error));
        }
    };

    const repairSortByParent = async () => {
        if (String(repairForm.parentId).trim() === "") {
            ElMessage.warning("请先填写父节点 ID");
            return;
        }
        try {
            repairResult.value = await api.post("/catalog/admin/repairSort", { parentId: Number(repairForm.parentId) });
            ElMessage.success("已完成指定父节点排序修复");
        } catch (error) {
            ElMessage.error("修复排序失败: " + extractErrorMessage(error));
        }
    };

    const repairAllSorts = async () => {
        try {
            repairResult.value = await api.post("/catalog/admin/repairSort/all");
            ElMessage.success("已完成整棵树排序修复");
        } catch (error) {
            ElMessage.error("整棵树排序修复失败: " + extractErrorMessage(error));
        }
    };

    return {
        queryBizPath,
        queryChildren,
        queryChildrenPage,
        queryBizTree,
        querySubtree,
        repairSelectedParentSorts,
        repairRootSorts,
        repairSortByParent,
        repairAllSorts
    };
}
