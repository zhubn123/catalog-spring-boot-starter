export function normalizeTree(nodes) {
    if (!Array.isArray(nodes) || nodes.length === 0) {
        return [];
    }

    return nodes
        .filter(Boolean)
        .map((node) => ({
            ...node,
            children: normalizeTree(node.children || [])
        }))
        .sort(compareNode);
}

export function toLazyTreeNodes(nodes) {
    if (!Array.isArray(nodes) || nodes.length === 0) {
        return [];
    }

    return nodes
        .filter(Boolean)
        .sort(compareNode)
        .map((node) => ({
            ...node,
            // 懒加载树初始只拿到基础节点信息，是否为叶子节点要等首次展开后才能确认。
            leaf: false,
            children: [],
            childrenLoaded: false
        }));
}

export function isLeaf(node) {
    if (typeof node?.leaf === "boolean") {
        return node.leaf;
    }
    return !node?.children || node.children.length === 0;
}

export function getBindingSummary(node) {
    return node?.extensions?.bindingSummary ?? null;
}

export function getBindingCount(node) {
    const count = getBindingSummary(node)?.count;
    return typeof count === "number" ? count : 0;
}

export function getBindingTypes(node) {
    const bizTypes = getBindingSummary(node)?.bizTypes;
    return Array.isArray(bizTypes) ? bizTypes : [];
}

export function getBindingPreview(node, limit = 2) {
    const bizIds = getBindingSummary(node)?.bizIds;
    if (!Array.isArray(bizIds) || bizIds.length === 0) {
        return "";
    }

    const previewItems = bizIds.slice(0, limit);
    const remainingCount = bizIds.length - previewItems.length;
    return remainingCount > 0
        ? `${previewItems.join("、")} 等 ${bizIds.length} 个业务对象`
        : previewItems.join("、");
}

export function findNodeById(nodes, nodeId) {
    if (!Array.isArray(nodes) || nodeId == null) {
        return null;
    }

    for (const node of nodes) {
        if (!node) {
            continue;
        }
        if (String(node.id) === String(nodeId)) {
            return node;
        }
        const child = findNodeById(node.children, nodeId);
        if (child) {
            return child;
        }
    }
    return null;
}

function compareNode(left, right) {
    const leftSort = typeof left?.sort === "number" ? left.sort : Number.MAX_SAFE_INTEGER;
    const rightSort = typeof right?.sort === "number" ? right.sort : Number.MAX_SAFE_INTEGER;
    if (leftSort !== rightSort) {
        return leftSort - rightSort;
    }

    const leftId = typeof left?.id === "number" ? left.id : Number.MAX_SAFE_INTEGER;
    const rightId = typeof right?.id === "number" ? right.id : Number.MAX_SAFE_INTEGER;
    return leftId - rightId;
}
