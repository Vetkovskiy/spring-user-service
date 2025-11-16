package com.userservice.exception;

/**
 * Исключение для случаев когда ресурс не найден (404)
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

}

