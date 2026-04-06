package io.github.zhubn123.catalog.autoconfigure;

import io.github.zhubn123.catalog.exception.CatalogException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogExceptionHandlerTest {

    private final CatalogExceptionHandler handler = new CatalogExceptionHandler();

    @Test
    void handleCatalogExceptionMapsNotFound() {
        ResponseEntity<CatalogErrorResponse> response = handler.handleCatalogException(CatalogException.nodeNotFound(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("NODE_NOT_FOUND");
        assertThat(response.getBody().message()).contains("1");
    }

    @Test
    void handleCatalogExceptionMapsConflict() {
        ResponseEntity<CatalogErrorResponse> response = handler.handleCatalogException(
                CatalogException.bizAlreadyBound("biz-1", "deliver", 10L)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("BIZ_ALREADY_BOUND");
    }

    @Test
    void handleCatalogExceptionMapsInvalidArgumentToBadRequest() {
        ResponseEntity<CatalogErrorResponse> response = handler.handleCatalogException(
                CatalogException.invalidArgument("bizType不能为空")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new CatalogErrorResponse("INVALID_ARGUMENT", "bizType不能为空"));
    }

    @Test
    void handleBadRequestReturnsStablePayload() {
        ResponseEntity<CatalogErrorResponse> response = handler.handleBadRequest(
                new HttpMessageNotReadableException("bad json")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new CatalogErrorResponse("INVALID_REQUEST", "bad json"));
    }

    @Test
    void handleUnexpectedExceptionReturnsInternalError() {
        ResponseEntity<CatalogErrorResponse> response = handler.handleUnexpectedException(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(new CatalogErrorResponse("INTERNAL_ERROR", "系统内部异常，请稍后重试"));
    }
}
