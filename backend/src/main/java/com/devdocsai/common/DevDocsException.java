package com.devdocsai.common;

import org.springframework.http.HttpStatus;

public class DevDocsException extends RuntimeException {
    private final String code;
    private final HttpStatus httpStatus;

    public DevDocsException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    // Static factory helpers for common cases
    public static DevDocsException notFound(String message) {
        return new DevDocsException("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }

    public static DevDocsException badRequest(String code, String message) {
        return new DevDocsException(code, message, HttpStatus.BAD_REQUEST);
    }

    public static DevDocsException conflict(String code, String message) {
        return new DevDocsException(code, message, HttpStatus.CONFLICT);
    }

    public static DevDocsException unauthorized(String code, String message) {
        return new DevDocsException(code, message, HttpStatus.UNAUTHORIZED);
    }

    public String getCode() { return code; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}
