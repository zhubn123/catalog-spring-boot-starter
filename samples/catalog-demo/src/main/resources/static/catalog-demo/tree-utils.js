export function normalizeTree(nodes) {
    if (!Array.isArray(nodes) || nodes.length === 0) {
        return [];
    }

    // 后端已经返回嵌套树，这里只做前端兜底清洗和排序。
    return nodes
        .filter(Boolean)
        .map((node) => ({
            ...node,
            children: normalizeTree(node.children || [])
        }))
        .sort(compareNode);
}

export function isLeaf(node) {
    if (typeof node?.leaf === "boolean") {
        return node.leaf;
    }
    return !node?.children || node.children.length === 0;
}

export function findNodeById(nodes, nodeId) {
    if (!Array.isArray(nodes) || nodeId == null) {
        return null;
    }

    // 刷新树后，需要在新树里重新定位旧的选中节点。
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
