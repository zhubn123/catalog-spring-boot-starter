package io.github.zhubn123.catalog.exception;

/**
 * 目录模块异常。
 *
 * <p>所有目录模块的业务异常统一通过此类抛出，便于上层统一处理。</p>
 *
 * @author zhubn
 * @date 2026/4/2
 */
public class CatalogException extends RuntimeException {

    private final String errorCode;

    public CatalogException(String message) {
        super(message);
        this.errorCode = "CATALOG_ERROR";
    }

    public CatalogException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CatalogException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static CatalogException nodeNotFound(Long nodeId) {
        return new CatalogException("NODE_NOT_FOUND", "节点不存在: " + nodeId);
    }

    public static CatalogException parentNotFound(Long parentId) {
        return new CatalogException("PARENT_NOT_FOUND", "父节点不存在: " + parentId);
    }

    public static CatalogException nameBlank() {
        return new CatalogException("NAME_BLANK", "节点名称不能为空");
    }

    public static CatalogException notLeafNode(Long nodeId) {
        return new CatalogException("NOT_LEAF_NODE", "只有叶子节点才能绑定业务对象: " + nodeId);
    }

    public static CatalogException cannotMoveToSelf(Long nodeId) {
        return new CatalogException("CANNOT_MOVE_TO_SELF", "不能将节点移动到自身或子节点: " + nodeId);
    }

    public static CatalogException hasChildren(Long nodeId) {
        return new CatalogException("HAS_CHILDREN", "节点存在子节点，无法删除: " + nodeId);
    }

    public static CatalogException hasBindings(Long nodeId) {
        return new CatalogException("HAS_BINDINGS", "节点存在业务绑定，无法删除: " + nodeId);
    }

    public static CatalogException invalidArgument(String message) {
        return new CatalogException("INVALID_ARGUMENT", message);
    }
}
