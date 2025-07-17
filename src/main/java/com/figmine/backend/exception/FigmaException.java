package com.figmine.backend.exception;

public class FigmaException extends RuntimeException {
    private final String code;
    private final String detail;

    public FigmaException(String code, String message) {
        super(message); // Pass message to RuntimeException
        this.code = code;
        this.detail = null;
    }

    public FigmaException(String code, String message, String detail) {
        super(message);
        this.code = code;
        this.detail = detail;
    }

    public String getCode() {
        return code;
    }

    public String getDetail() {
        return detail;
    }
}
