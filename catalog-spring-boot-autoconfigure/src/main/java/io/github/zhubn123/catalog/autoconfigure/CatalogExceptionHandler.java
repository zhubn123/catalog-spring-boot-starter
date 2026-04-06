package io.github.zhubn123.catalog.autoconfigure;

import io.github.zhubn123.catalog.exception.CatalogException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;

/**
 * Catalog REST API 异常处理器。
 *
 * <p>统一将目录模块抛出的业务异常和常见请求错误转换为稳定的 HTTP 状态码与响应体，
 * 避免前端直接依赖 Spring 默认错误页或原始堆栈信息。</p>
 */
@RestControllerAdvice
public class CatalogExceptionHandler {

    /**
     * 应映射为 404 的业务错误码。
     */
    private static final Set<String> NOT_FOUND_CODES = Set.of("NODE_NOT_FOUND", "PARENT_NOT_FOUND");

    /**
     * 应映射为 409 的业务错误码。
     */
    private static final Set<String> CONFLICT_CODES = Set.of(
            "NOT_LEAF_NODE",
            "CANNOT_MOVE_TO_SELF",
            "HAS_CHILDREN",
            "HAS_BINDINGS",
            "BIZ_ALREADY_BOUND",
            "BIZ_BOUND_TO_MULTIPLE_NODES"
    );

    /**
     * 处理目录模块的业务异常。
     */
    @ExceptionHandler(CatalogException.class)
    public ResponseEntity<CatalogErrorResponse> handleCatalogException(CatalogException exception) {
        HttpStatus status = resolveStatus(exception.getErrorCode());
        return ResponseEntity.status(status)
                .body(new CatalogErrorResponse(exception.getErrorCode(), exception.getMessage()));
    }

    /**
     * 处理请求体解析失败、参数类型不匹配等客户端输入问题。
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<CatalogErrorResponse> handleBadRequest(Exception exception) {
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? "请求参数或请求体无效"
                : exception.getMessage();
        return ResponseEntity.badRequest()
                .body(new CatalogErrorResponse("INVALID_REQUEST", message));
    }

    /**
     * 兜底处理未预期异常，避免把内部堆栈细节直接暴露给调用方。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CatalogErrorResponse> handleUnexpectedException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CatalogErrorResponse("INTERNAL_ERROR", "系统内部异常，请稍后重试"));
    }

    /**
     * 根据稳定错误码选择更合适的 HTTP 状态码。
     */
    private HttpStatus resolveStatus(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return HttpStatus.BAD_REQUEST;
        }
        if (NOT_FOUND_CODES.contains(errorCode)) {
            return HttpStatus.NOT_FOUND;
        }
        if (CONFLICT_CODES.contains(errorCode)) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
