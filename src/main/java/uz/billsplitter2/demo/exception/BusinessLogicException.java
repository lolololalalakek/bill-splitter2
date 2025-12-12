package uz.billsplitter2.demo.exception;

import org.springframework.http.HttpStatus;

public class BusinessLogicException extends ApplicationException {

    private static final String CODE = "BUSINESS_RULE_VIOLATION";

    public BusinessLogicException(String message) {
        super(message, HttpStatus.CONFLICT, CODE);
    }
}
