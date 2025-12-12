package uz.billsplitter2.demo.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends ApplicationException {
    private static final String CODE = "VALIDATION_ERROR";

    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, CODE);
    }
}
