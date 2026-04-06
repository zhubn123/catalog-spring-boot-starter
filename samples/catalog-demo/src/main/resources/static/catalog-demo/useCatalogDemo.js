import { createApi, extractErrorMessage } from "./api.js";
import { findNodeById, isLeaf, normalizeTree } from "./tree-utils.js";
import { createNodeActions } from "./modules/node-operations.js";
import { createBindingActions } from "./modules/binding-operations.js";
import { createQueryActions } from "./modules/query-operations.js";
import { createBusinessActions } from "./modules/business-operations.js";

const { ref, reactive, computed, watch, onMounted } = Vue;
const { ElMessage } = ElementPlus;

function resolveApiBase() {
    // 从 8080 打开时走同源；直接预览静态文件时回退到本地后端。
    if (window.location.port === "8080") {
        return window.location.origin;
    }
    return "http://localhost:8080";
}

const API_BASE = resolveApiBase();

export function createCatalogDemoApp() {
    return {
        setup() {
            // 左侧目录树和右侧详情共享一份选中节点状态，避免各区域各自维护。
            const treeRef = ref(null);
            const treeData = ref([]);
            const selectedNode = ref(null);
            const activeTab = ref("node");
            const apiLogs = ref([]);
            const directBindings = ref([]);
            const bindingListBizType = ref("deliver");
            const queryTreeData = ref([]);
            const queryTreeTitle = ref("");
            const queryTreeSummary = ref("");
            const contractNodeId = ref("");

            const treeProps = {
                children: "children",
                label: "name"
            };

            const addForm = reactive({ parentId: "", name: "" });
            const batchAddForm = reactive({ parentId: "", names: "" });
            const updateForm = reactive({ nodeId: "", name: "", code: "", sort: "" });
            const bindForm = reactive({ nodeId: "", bizId: "", bizType: "deliver" });
            const batchBindForm = reactive({ nodeIds: "", bizIds: "", bizType: "deliver" });

            const queryBizPathForm = reactive({ bizId: "", bizType: "deliver" });
            const bizPathResult = ref([]);
            const queryBizNodesForm = reactive({ bizId: "", bizType: "deliver" });
            const bizNodesResult = ref([]);
            const queryNodeBizForm = reactive({ nodeId: "", bizType: "deliver" });
            const nodeBizResult = ref([]);
            const queryBizTreeForm = reactive({ bizId: "", bizType: "deliver" });
            const querySubtreeForm = reactive({ nodeId: "" });

            const contractForm = reactive({
                contractId: "",
                contractName: "",
                items: [{ deliveryId: "", deliveryType: "" }]
            });
            const attachForm = reactive({ projectNodeId: "", contractNodeId: "" });

            const selectedNodeId = computed(() => selectedNode.value?.id ?? "");

            const logApi = (method, url, success, status) => {
                const time = new Date().toLocaleTimeString();
                apiLogs.value.unshift({ method, url, success, status, time });
                if (apiLogs.value.length > 80) {
                    apiLogs.value.pop();
                }
            };

            const clearApiLogs = () => {
                apiLogs.value = [];
            };

            const api = createApi({ baseUrl: API_BASE, logApi });

            const clearQueryTree = () => {
                queryTreeData.value = [];
                queryTreeTitle.value = "";
                queryTreeSummary.value = "";
            };

            const clearSelection = () => {
                selectedNode.value = null;
                directBindings.value = [];
                addForm.parentId = "";
                batchAddForm.parentId = "";
                updateForm.nodeId = "";
                updateForm.name = "";
                updateForm.code = "";
                updateForm.sort = "";
                bindForm.nodeId = "";
                queryNodeBizForm.nodeId = "";
                querySubtreeForm.nodeId = "";
            };

            const syncFormsFromSelectedNode = (node) => {
                if (!node) {
                    clearSelection();
                    return;
                }

                selectedNode.value = node;
                addForm.parentId = String(node.id ?? "");
                batchAddForm.parentId = String(node.id ?? "");
                updateForm.nodeId = String(node.id ?? "");
                updateForm.name = node.name || "";
                updateForm.code = node.code || "";
                updateForm.sort = node.sort ?? "";
                bindForm.nodeId = String(node.id ?? "");
                queryNodeBizForm.nodeId = String(node.id ?? "");
                querySubtreeForm.nodeId = String(node.id ?? "");
            };

            const loadDirectBindings = async (nodeId = selectedNodeId.value) => {
                if (!nodeId) {
                    directBindings.value = [];
                    return;
                }
                try {
                    // 这里查的是当前节点的直接绑定，不再混用子树聚合查询。
                    const bizIds = await api.get(
                        `/catalog/nodeBindings?nodeId=${nodeId}&bizType=${bindingListBizType.value}`
                    );
                    directBindings.value = (bizIds || []).map((bizId) => ({
                        bizId,
                        bizType: bindingListBizType.value
                    }));
                } catch (error) {
                    directBindings.value = [];
                    ElMessage.error("加载当前节点绑定失败: " + extractErrorMessage(error));
                }
            };

            const loadTree = async ({ preserveSelection = true } = {}) => {
                const previousNodeId = preserveSelection ? selectedNodeId.value : "";
                try {
                    const tree = normalizeTree(await api.get("/catalog/tree"));
                    treeData.value = tree;

                    if (previousNodeId) {
                        const matchedNode = findNodeById(tree, previousNodeId);
                        if (matchedNode) {
                            syncFormsFromSelectedNode(matchedNode);
                            await loadDirectBindings(matchedNode.id);
                            return;
                        }
                    }

                    if (previousNodeId) {
                        // 原选中节点如果已经不在新树里，右侧表单也要一起清掉。
                        clearSelection();
                    } else if (tree.length === 0) {
                        clearSelection();
                    } else if (!preserveSelection) {
                        clearSelection();
                    }
                } catch (error) {
                    ElMessage.error("加载目录树失败: " + extractErrorMessage(error));
                }
            };

            const handleNodeClick = (node) => {
                syncFormsFromSelectedNode(node);
                loadDirectBindings(node.id);
            };

            const allowDrop = (_draggingNode, dropNode, type) => type !== "inner" || !isLeaf(dropNode.data);
            const allowDrag = () => true;

            const handleDrop = async (draggingNode, dropNode, dropType) => {
                const draggingNodeId = draggingNode.data.id;
                let targetParentId = null;
                let targetIndex = null;

                if (dropType === "inner") {
                    targetParentId = dropNode.data.id;
                } else {
                    targetParentId = dropNode.data.parentId || 0;
                    const siblings = dropNode.parent.childNodes;
                    const dropIndex = siblings.indexOf(dropNode);
                    targetIndex = dropType === "after" ? dropIndex : dropIndex - 1;
                }

                try {
                    const payload = { nodeId: draggingNodeId };
                    if (targetParentId) {
                        payload.parentId = targetParentId;
                    }
                    if (targetIndex !== null && targetIndex >= 0) {
                        payload.targetIndex = targetIndex;
                    }
                    await api.postJson("/catalog/move", payload);
                    ElMessage.success("节点移动成功");
                    await loadTree();
                } catch (error) {
                    ElMessage.error("节点移动失败: " + extractErrorMessage(error));
                    await loadTree();
                }
            };

            const expandAll = () => {
                const nodes = treeRef.value?.store?.nodesMap;
                if (!nodes) {
                    return;
                }
                Object.values(nodes).forEach((node) => {
                    node.expanded = true;
                });
            };

            const collapseAll = () => {
                const nodes = treeRef.value?.store?.nodesMap;
                if (!nodes) {
                    return;
                }
                Object.values(nodes).forEach((node) => {
                    node.expanded = false;
                });
            };

            watch(bindingListBizType, async () => {
                if (selectedNode.value) {
                    await loadDirectBindings(selectedNode.value.id);
                }
            });

            const sharedContext = {
                api,
                ElMessage,
                activeTab,
                selectedNode,
                selectedNodeId,
                treeData,
                addForm,
                batchAddForm,
                updateForm,
                bindForm,
                batchBindForm,
                directBindings,
                bindingListBizType,
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
                contractForm,
                contractNodeId,
                attachForm,
                loadTree,
                loadDirectBindings,
                clearQueryTree,
                clearSelection,
                syncFormsFromSelectedNode
            };

            const nodeActions = createNodeActions({
                ...sharedContext,
                ElMessageBox: ElementPlus.ElMessageBox
            });
            const bindingActions = createBindingActions(sharedContext);
            const queryActions = createQueryActions(sharedContext);
            const businessActions = createBusinessActions(sharedContext);

            onMounted(() => {
                loadTree({ preserveSelection: false });
            });

            return {
                treeRef,
                treeData,
                treeProps,
                selectedNode,
                selectedNodeId,
                activeTab,
                apiLogs,
                directBindings,
                bindingListBizType,
                queryTreeData,
                queryTreeTitle,
                queryTreeSummary,
                contractNodeId,
                addForm,
                batchAddForm,
                updateForm,
                bindForm,
                batchBindForm,
                queryBizPathForm,
                bizPathResult,
                queryBizNodesForm,
                bizNodesResult,
                queryNodeBizForm,
                nodeBizResult,
                queryBizTreeForm,
                querySubtreeForm,
                contractForm,
                attachForm,
                clearApiLogs,
                loadTree,
                loadDirectBindings,
                isLeaf,
                handleNodeClick,
                allowDrop,
                allowDrag,
                handleDrop,
                expandAll,
                collapseAll,
                ...nodeActions,
                ...bindingActions,
                ...queryActions,
                ...businessActions
            };
        }
    };
}
