package com.sayless.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.sayless.contract.ApiErrorResponse;
import com.sayless.contract.ErrorCode;
import com.sayless.contract.RewriteException;

/**
 * Every error body uses only canonical ErrorCode messages. Exception messages are never logged or
 * returned because they can contain user text, provider output or configuration values.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RewriteException.class)
    ResponseEntity<Object> handleRewrite(RewriteException ex) {
        log.info("Rewrite failed: code={} status={}", ex.code(), ex.code().httpStatus());
        return error(ex.code(), new HttpHeaders());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Unexpected error: code={} type={}", ErrorCode.INTERNAL_ERROR, ex.getClass().getName());
        return error(ErrorCode.INTERNAL_ERROR, new HttpHeaders());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return error(ErrorCode.INVALID_JSON, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return error(ErrorCode.UNSUPPORTED_MEDIA_TYPE, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return error(ErrorCode.METHOD_NOT_ALLOWED, headers);
    }

    private static ResponseEntity<Object> error(ErrorCode code, HttpHeaders frameworkHeaders) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(frameworkHeaders);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setCacheControl(CacheControl.noStore());
        ApiErrorResponse body = new ApiErrorResponse(new ApiErrorResponse.ApiError(code.name(), code.publicMessage()));
        return ResponseEntity.status(code.httpStatus()).headers(headers).body(body);
    }
}
