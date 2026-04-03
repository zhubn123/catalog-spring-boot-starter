package com.example.demo.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("请求路径：{}", request.getRequestURI(), e);
        return Result.fail(ResultCode.SYSTEM_ERROR.getCode(), "系统内部错误�? + e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("请求路径：{}", request.getRequestURI(), e);
        return Result.fail(ResultCode.BUSINESS_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.error("请求路径：{}", request.getRequestURI(), e);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "参数错误�? + e.getMessage());
    }
}
