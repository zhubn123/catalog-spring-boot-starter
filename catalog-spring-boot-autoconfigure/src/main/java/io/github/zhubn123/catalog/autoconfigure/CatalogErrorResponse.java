package io.github.zhubn123.catalog.autoconfigure;

/**
 * Catalog REST API 错误响应。
 *
 * @param code    稳定错误码
 * @param message 面向调用方的错误消息
 */
public record CatalogErrorResponse(String code, String message) {
}
