package com.api.moviebooking.helpers.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to delete a parent entity that has dependent
 * children.
 * This enforces business rules around data integrity and cascading deletes.
 */
public class EntityDeletionForbiddenException extends CustomException {

    public EntityDeletionForbiddenException() {
        super("Cannot delete parent when it has associated children.", HttpStatus.CONFLICT);
    }
}
