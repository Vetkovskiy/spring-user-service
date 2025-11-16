package com.userservice.exception;

/**
* Исключение для случаев дублирования ресурса (409 Conflict)
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

}

