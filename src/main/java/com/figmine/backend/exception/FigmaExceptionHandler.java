package com.figmine.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.figmine.backend.dto.ErrorResponse;
@ControllerAdvice
public class FigmaExceptionHandler {
    
    @ExceptionHandler(FigmaException.class)
    public ResponseEntity<ErrorResponse> handleFigmaException(FigmaException ex) {
        return new ResponseEntity<>(
            ErrorResponse.builder()
                .code(ex.getCode())
                .message(ex.getMessage())
                .detail(ex.getDetail())
                .build(),
            HttpStatus.BAD_REQUEST
        );
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        return new ResponseEntity<>(
            ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .detail(ex.getMessage())
                .build(),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
