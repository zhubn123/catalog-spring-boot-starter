import { createApi } from "./api.js";
import { buildTree, isLeaf } from "./tree-utils.js";
import { createNodeActions } from "./modules/node-operations.js";
import { createBindingActions } from "./modules/binding-operations.js";
import { createQueryActions } from "./modules/query-operations.js";
import { createBusinessActions } from "./modules/business-operations.js";

const { ref, reactive, onMounted } = Vue;
const { ElMessage } = ElementPlus;

function resolveApiBase() {
    if (window.location.port === "8080") {
        return window.location.origin;
    }
    return "http://localhost:8080";
}

const API_BASE = resolveApiBase();

export function createCatalogDemoApp() {
    return {
        setup() {
            const treeRef = ref(null);
            const treeData = ref([]);
            const selectedNode = ref(null);
            const activeTab = ref("node");
            const apiLogs = ref([]);
            const nodeBindings = ref([]);
            const contractNodeId = ref("");
            const editDialogVisible = ref(false);

            const treeProps = {
                children: "children",
                label: "name"
            };

            const addForm = reactive({ parentId: "", name: "" });
            const batchAddForm = reactive({ parentId: "", names: "" });
            const moveForm = reactive({ nodeId: "", parentId: "", targetIndex: "" });
            const updateForm = reactive({ nodeId: "", name: "", code: "", sort: "" });
            const bindForm = reactive({ nodeId: "", bizId: "", bizType: "deliver" });
            const batchBindForm = reactive({ nodeIds: "", bizIds: "", bizType: "deliver" });
            const unbindForm = reactive({ nodeId: "", bizId: "", bizType: "deliver" });

            const queryBizPathForm = reactive({ bizId: "", bizType: "deliver" });
            const bizPathResult = ref([]);
            const queryBizNodesForm = reactive({ bizId: "", bizType: "deliver" });
            const bizNodesResult = ref([]);
            const queryNodeBizForm = reactive({ nodeId: "", bizType: "deliver" });
            const nodeBizResult = ref([]);
            const queryBizTreeForm = reactive({ bizId: "", bizType: "deliver" });
            const bizTreeResult = ref([]);
            const querySubtreeForm = reactive({ nodeId: "" });
            const subtreeResult = ref([]);

            const contractForm = reactive({
                contractId: "",
                contractName: "",
                items: [{ deliveryId: "", deliveryType: "" }]
            });
            const attachForm = reactive({ projectNodeId: "", contractNodeId: "" });
            const editForm = reactive({ id: "", name: "", code: "", sort: null });

            const logApi = (method, url, success, status) => {
                const time = new Date().toLocaleTimeString();
                apiLogs.value.unshift({ method, url, success, status, time });
                if (apiLogs.value.length > 50) {
                    apiLogs.value.pop();
                }
            };

            const clearApiLogs = () => {
                apiLogs.value = [];
            };

            const api = createApi({ baseUrl: API_BASE, logApi });

            const loadTree = async () => {
                try {
                    const nodes = await api.get("/catalog/tree");
                    treeData.value = buildTree(nodes);
                } catch (error) {
                    ElMessage.error("加载树失败: " + (error.response?.data?.message || error.message));
                }
            };

            const loadNodeBindings = async (nodeId) => {
                try {
                    const bizIds = await api.get(`/catalog/nodeBiz?nodeId=${nodeId}&bizType=deliver`);
                    nodeBindings.value = bizIds.map((bizId) => ({ bizId, bizType: "deliver" }));
                } catch (_error) {
                    nodeBindings.value = [];
                }
            };

            const handleNodeClick = (node) => {
                selectedNode.value = node;
                addForm.parentId = node.id;
                updateForm.nodeId = node.id;
                updateForm.name = node.name;
                updateForm.code = node.code || "";
                bindForm.nodeId = node.id;
                queryNodeBizForm.nodeId = node.id;
                loadNodeBindings(node.id);
            };

            const allowDrop = (_draggingNode, dropNode, type) => {
                return type !== "inner" || !isLeaf(dropNode.data);
            };

            const allowDrag = () => true;

            const handleDrop = async (draggingNode, dropNode, dropType) => {
                const dragId = draggingNode.data.id;
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
                    const params = { nodeId: dragId };
                    if (targetParentId) {
                        params.parentId = targetParentId;
                    }
                    if (targetIndex !== null && targetIndex >= 0) {
                        params.targetIndex = targetIndex;
                    }
                    await api.post("/catalog/move", params);
                    ElMessage.success("移动成功");
                    await loadTree();
                } catch (error) {
                    ElMessage.error("移动失败: " + (error.response?.data?.message || error.message));
                    await loadTree();
                }
            };

            const expandAll = () => {
                const nodes = treeRef.value?.store?.nodesMap;
                if (nodes) {
                    Object.values(nodes).forEach((node) => {
                        node.expanded = true;
                    });
                }
            };

            const collapseAll = () => {
                const nodes = treeRef.value?.store?.nodesMap;
                if (nodes) {
                    Object.values(nodes).forEach((node) => {
                        node.expanded = false;
                    });
                }
            };

            const sharedContext = {
                api,
                ElMessage,
                selectedNode,
                activeTab,
                addForm,
                batchAddForm,
                moveForm,
                updateForm,
                bindForm,
                batchBindForm,
                unbindForm,
                queryBizPathForm,
                bizPathResult,
                queryBizNodesForm,
                bizNodesResult,
                queryNodeBizForm,
                nodeBizResult,
                queryBizTreeForm,
                bizTreeResult,
                querySubtreeForm,
                subtreeResult,
                contractForm,
                contractNodeId,
                attachForm,
                editDialogVisible,
                editForm,
                loadTree,
                loadNodeBindings
            };

            const nodeActions = createNodeActions({
                ...sharedContext,
                ElMessageBox: ElementPlus.ElMessageBox
            });
            const bindingActions = createBindingActions(sharedContext);
            const queryActions = createQueryActions(sharedContext);
            const businessActions = createBusinessActions(sharedContext);

            onMounted(() => {
                loadTree();
            });

            return {
                treeRef,
                treeData,
                treeProps,
                selectedNode,
                activeTab,
                apiLogs,
                nodeBindings,
                contractNodeId,
                editDialogVisible,
                addForm,
                batchAddForm,
                moveForm,
                updateForm,
                bindForm,
                batchBindForm,
                unbindForm,
                queryBizPathForm,
                bizPathResult,
                queryBizNodesForm,
                bizNodesResult,
                queryNodeBizForm,
                nodeBizResult,
                queryBizTreeForm,
                bizTreeResult,
                querySubtreeForm,
                subtreeResult,
                contractForm,
                attachForm,
                editForm,
                clearApiLogs,
                loadTree,
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
