package com.coworkhub.business;

import org.springframework.http.HttpStatus;

public final class Errors {
    private Errors() {}
    public static BusinessException notFound(String resource) {
        return new BusinessException(resource + " no encontrado", "NOT_FOUND", HttpStatus.NOT_FOUND);
    }
    public static BusinessException conflict(String message, String code) {
        return new BusinessException(message, code, HttpStatus.CONFLICT);
    }
    public static BusinessException invalid(String message, String code) {
        return new BusinessException(message, code, HttpStatus.BAD_REQUEST);
    }
    public static BusinessException forbidden(String message) {
        return new BusinessException(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }
}
