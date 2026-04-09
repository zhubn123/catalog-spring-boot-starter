import { createApi, extractErrorMessage } from "./api.js";
import { getBindingCount, getBindingTypes, isLeaf, toLazyTreeNodes } from "./tree-utils.js";
import { createNodeActions } from "./modules/node-operations.js";
import { createBindingActions } from "./modules/binding-operations.js";
import { createQueryActions } from "./modules/query-operations.js";
import { createBusinessActions } from "./modules/business-operations.js";

const { ref, reactive, computed, watch, onMounted, nextTick } = Vue;
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
            const directBindings = ref([]);
            const bindingListBizType = ref("deliver");
            const queryTreeData = ref([]);
            const queryTreeTitle = ref("");
            const queryTreeSummary = ref("");
            const contractNodeId = ref("");
            const apiBase = API_BASE;
            const childrenItems = ref([]);
            const childrenMeta = ref(null);
            const repairResult = ref(null);
            const bizPathResult = ref([]);
            const rootPage = reactive({ page: 1, size: 10, total: 0, hasNext: false, loading: false });

            const treeProps = { children: "children", label: "name", isLeaf: "leaf" };

            const addForm = reactive({ parentId: "", name: "" });
            const updateForm = reactive({ nodeId: "", name: "", code: "", sort: "" });
            const bindForm = reactive({ nodeId: "", bizId: "", bizType: "deliver" });
            const bindManyForm = reactive({ bizIdsText: "" });
            const childrenQueryForm = reactive({ parentId: "", page: 1, size: 20 });
            const repairForm = reactive({ parentId: "" });
            const queryBizPathForm = reactive({ bizId: "", bizType: "deliver" });
            const queryBizTreeForm = reactive({ bizId: "", bizType: "deliver" });
            const querySubtreeForm = reactive({ nodeId: "" });
            const contractForm = reactive({
                contractId: "",
                contractName: "",
                items: [{ deliveryId: "", deliveryType: "" }]
            });
            const attachForm = reactive({ projectNodeId: "", contractNodeId: "" });

            const selectedNodeId = computed(() => selectedNode.value?.id ?? "");
            const rootPageSummary = computed(() => {
                if (rootPage.total === 0) {
                    return `根节点分页：第 ${rootPage.page} 页，当前暂无数据`;
                }
                return `根节点分页：第 ${rootPage.page} 页 / 每页 ${rootPage.size} 条 / 共 ${rootPage.total} 条`;
            });
            const canLoadPreviousRootPage = computed(() => rootPage.page > 1);
            const canLoadNextRootPage = computed(() => rootPage.hasNext);

            const logApi = (entry) => {
                apiLogs.value.unshift(entry);
                if (apiLogs.value.length > 120) {
                    apiLogs.value.pop();
                }
            };

            const clearApiLogs = () => {
                apiLogs.value = [];
            };

            const hasLogText = (value) => typeof value === "string" && value.trim().length > 0;
            const getApiLogStatusText = (log) => log.success ? "成功" : "失败";
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
                updateForm.nodeId = "";
                updateForm.name = "";
                updateForm.code = "";
                updateForm.sort = "";
                bindForm.nodeId = "";
                querySubtreeForm.nodeId = "";
            };

            const syncFormsFromSelectedNode = (node) => {
                if (!node) {
                    clearSelection();
                    return;
                }
                selectedNode.value = node;
                addForm.parentId = String(node.id ?? "");
                updateForm.nodeId = String(node.id ?? "");
                updateForm.name = node.name || "";
                updateForm.code = node.code || "";
                updateForm.sort = node.sort ?? "";
                bindForm.nodeId = String(node.id ?? "");
                querySubtreeForm.nodeId = String(node.id ?? "");
            };

            // 懒加载树刷新时只会重新拉根节点分页，这里保留旧 path 以便在刷新后按需恢复选中链路。
            const createSelectionSnapshot = () => {
                if (!selectedNode.value?.id || !selectedNode.value?.path) {
                    return null;
                }
                const pathIds = String(selectedNode.value.path)
                    .split("/")
                    .filter(Boolean)
                    .map((item) => Number(item))
                    .filter((item) => Number.isInteger(item) && item > 0);
                if (pathIds.length === 0) {
                    return null;
                }
                return { id: Number(selectedNode.value.id), pathIds };
            };

            const loadDirectBindings = async (nodeId = selectedNodeId.value) => {
                if (!nodeId) {
                    directBindings.value = [];
                    return;
                }
                try {
                    const bizIds = await api.get(`/catalog/nodeBindings?nodeId=${nodeId}&bizType=${bindingListBizType.value}`);
                    directBindings.value = (bizIds || []).map((bizId) => ({ bizId, bizType: bindingListBizType.value }));
                } catch (error) {
                    directBindings.value = [];
                    ElMessage.error("加载当前节点绑定失败: " + extractErrorMessage(error));
                }
            };

            const fetchDirectChildren = async (parentId) => {
                return toLazyTreeNodes(await api.get(`/catalog/children?parentId=${parentId}`));
            };

            // 同一个节点只在首次展开时查询直接子节点，后续复用已加载的分支数据。
            const ensureNodeChildrenLoaded = async (node) => {
                if (!node) {
                    return [];
                }
                if (node.childrenLoaded) {
                    return Array.isArray(node.children) ? node.children : [];
                }
                const children = await fetchDirectChildren(node.id);
                node.children = children;
                node.childrenLoaded = true;
                node.leaf = children.length === 0;
                return children;
            };

            const expandTreePath = async (pathIds) => {
                for (const nodeId of pathIds.slice(0, -1)) {
                    await nextTick();
                    const treeNode = treeRef.value?.getNode?.(nodeId) ?? treeRef.value?.store?.nodesMap?.[nodeId];
                    if (treeNode) {
                        treeNode.expanded = true;
                    }
                }
                await nextTick();
                treeRef.value?.setCurrentKey?.(pathIds[pathIds.length - 1]);
            };

            const restoreSelection = async (snapshot) => {
                if (!snapshot?.pathIds?.length) {
                    return false;
                }
                let currentNode = treeData.value.find((item) => String(item.id) === String(snapshot.pathIds[0]));
                if (!currentNode) {
                    return false;
                }
                for (let index = 1; index < snapshot.pathIds.length; index += 1) {
                    const children = await ensureNodeChildrenLoaded(currentNode);
                    currentNode = children.find((item) => String(item.id) === String(snapshot.pathIds[index]));
                    if (!currentNode) {
                        return false;
                    }
                }
                syncFormsFromSelectedNode(currentNode);
                await loadDirectBindings(currentNode.id);
                await expandTreePath(snapshot.pathIds);
                return true;
            };

            const loadTree = async ({ page = rootPage.page, preserveSelection = true } = {}) => {
                const selectionSnapshot = preserveSelection ? createSelectionSnapshot() : null;
                const targetPage = Math.max(1, Number(page || 1));
                try {
                    rootPage.loading = true;
                    // 根节点现在统一走分页接口，避免 sample 再退回整棵树全量读取。
                    const result = await api.get(`/catalog/childrenPage?parentId=0&page=${targetPage}&size=${rootPage.size}`);
                    treeData.value = toLazyTreeNodes(result.items || []);
                    rootPage.page = Number(result.page || targetPage);
                    rootPage.size = Number(result.size || rootPage.size);
                    rootPage.total = Number(result.total || 0);
                    rootPage.hasNext = Boolean(result.hasNext);
                    if (selectionSnapshot && await restoreSelection(selectionSnapshot)) {
                        return;
                    }
                    treeRef.value?.setCurrentKey?.(null);
                    if (!preserveSelection || selectionSnapshot || treeData.value.length === 0) {
                        clearSelection();
                    }
                } catch (error) {
                    ElMessage.error("加载目录树失败: " + extractErrorMessage(error));
                } finally {
                    rootPage.loading = false;
                }
            };

            const refreshTree = () => loadTree({ page: rootPage.page, preserveSelection: true });
            const loadPreviousRootPage = () => loadTree({ page: rootPage.page - 1, preserveSelection: false });
            const loadNextRootPage = () => loadTree({ page: rootPage.page + 1, preserveSelection: false });

            const loadTreeNode = async (treeNode, resolve) => {
                if (treeNode.level === 0) {
                    resolve(treeData.value);
                    return;
                }
                try {
                    const children = await ensureNodeChildrenLoaded(treeNode.data);
                    resolve(children);
                } catch (error) {
                    ElMessage.error("加载子节点失败: " + extractErrorMessage(error));
                    resolve([]);
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
                    await loadTree({ preserveSelection: false });
                } catch (error) {
                    ElMessage.error("节点移动失败: " + extractErrorMessage(error));
                    await loadTree({ preserveSelection: false });
                }
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
                updateForm,
                bindForm,
                bindManyForm,
                directBindings,
                bindingListBizType,
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
                contractForm,
                contractNodeId,
                attachForm,
                loadTree,
                loadDirectBindings,
                clearQueryTree,
                clearSelection,
                syncFormsFromSelectedNode
            };

            const nodeActions = createNodeActions({ ...sharedContext, ElMessageBox: ElementPlus.ElMessageBox });
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
                rootPage,
                rootPageSummary,
                canLoadPreviousRootPage,
                canLoadNextRootPage,
                selectedNode,
                activeTab,
                apiLogs,
                apiBase,
                directBindings,
                bindingListBizType,
                queryTreeData,
                queryTreeTitle,
                queryTreeSummary,
                contractNodeId,
                addForm,
                updateForm,
                bindForm,
                bindManyForm,
                childrenQueryForm,
                childrenItems,
                childrenMeta,
                repairForm,
                repairResult,
                queryBizPathForm,
                bizPathResult,
                queryBizTreeForm,
                querySubtreeForm,
                contractForm,
                attachForm,
                clearApiLogs,
                hasLogText,
                getApiLogStatusText,
                loadTree,
                loadDirectBindings,
                isLeaf,
                getBindingCount,
                getBindingTypes,
                handleNodeClick,
                allowDrop,
                allowDrag,
                handleDrop,
                loadTreeNode,
                refreshTree,
                loadPreviousRootPage,
                loadNextRootPage,
                collapseAll,
                ...nodeActions,
                ...bindingActions,
                ...queryActions,
                ...businessActions
            };
        }
    };
}
