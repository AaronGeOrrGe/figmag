package com.figmine.backend.exception;

public class FigmaException extends RuntimeException {
    private final String code;
    private final String message;
    private final String detail;

    public FigmaException(String code, String message) {
        this.code = code;
        this.message = message;
        this.detail = null;
    }

    public FigmaException(String code, String message, String detail) {
        this.code = code;
        this.message = message;
        this.detail = detail;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDetail() {
        return detail;
    }
}
