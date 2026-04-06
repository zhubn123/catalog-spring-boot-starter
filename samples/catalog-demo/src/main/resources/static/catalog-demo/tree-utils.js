export function buildTree(nodes) {
    if (!nodes || nodes.length === 0) {
        return [];
    }

    const map = {};
    const roots = [];

    nodes.forEach((node) => {
        map[node.id] = { ...node, children: [] };
    });

    nodes.forEach((node) => {
        const current = map[node.id];
        if (!node.parentId || node.parentId <= 0) {
            roots.push(current);
        } else if (map[node.parentId]) {
            map[node.parentId].children.push(current);
        }
    });

    const sortChildren = (items) => {
        items.sort((left, right) => (left.sort || 0) - (right.sort || 0));
        items.forEach((item) => sortChildren(item.children));
    };

    sortChildren(roots);
    return roots;
}

export function isLeaf(node) {
    return !node.children || node.children.length === 0;
}
