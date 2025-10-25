package com.api.moviebooking.helpers.exceptions;

/**
 * Exception thrown when a requested resource is not found in the database.
 * This will be mapped to HTTP 404 Not Found status.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
