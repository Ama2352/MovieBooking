package com.api.moviebooking.helpers.exceptions;

import java.util.Date;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * Handle validation errors from @Valid annotation
         * Returns HTTP 400 Bad Request
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<CustomError> handleValidationException(
                        MethodArgumentNotValidException exception, WebRequest webRequest) {
                String errorMessage = exception.getBindingResult().getFieldErrors().stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                CustomError errorDetails = new CustomError(new Date(), errorMessage,
                                webRequest.getDescription(false));
                return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle resource not found errors
         * Returns HTTP 404 Not Found
         */
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<CustomError> handleResourceNotFoundException(
                        ResourceNotFoundException exception, WebRequest webRequest) {
                CustomError errorDetails = new CustomError(new Date(), exception.getMessage(),
                                webRequest.getDescription(false));
                return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
        }

        /**
         * Handle seat locked exceptions from booking
         * Returns HTTP 409 Conflict
         */
        @ExceptionHandler(SeatLockedException.class)
        public ResponseEntity<CustomError> handleSeatLockedException(
                        SeatLockedException exception, WebRequest webRequest) {
                CustomError errorDetails = new CustomError(new Date(), exception.getMessage(),
                                webRequest.getDescription(false));
                return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT);
        }

        /**
         * Handle concurrent booking exceptions
         * Returns HTTP 409 Conflict
         */
        @ExceptionHandler(ConcurrentBookingException.class)
        public ResponseEntity<CustomError> handleConcurrentBookingException(
                        ConcurrentBookingException exception, WebRequest webRequest) {
                CustomError errorDetails = new CustomError(new Date(), exception.getMessage(),
                                webRequest.getDescription(false));
                return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT);
        }

        /**
         * Handle max seats exceeded exceptions
         * Returns HTTP 400 Bad Request
         */
        @ExceptionHandler(MaxSeatsExceededException.class)
        public ResponseEntity<CustomError> handleMaxSeatsExceededException(
                        MaxSeatsExceededException exception, WebRequest webRequest) {
                CustomError errorDetails = new CustomError(new Date(), exception.getMessage(),
                                webRequest.getDescription(false));
                return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle lock expired exceptions
         * Returns HTTP 410 Gone
         */
        @ExceptionHandler(LockExpiredException.class)
        public ResponseEntity<CustomError> handleLockExpiredException(
                        LockExpiredException exception, WebRequest webRequest) {
                CustomError errorDetails = new CustomError(new Date(), exception.getMessage(),
                                webRequest.getDescription(false));
                return new ResponseEntity<>(errorDetails, HttpStatus.GONE);
        }

        /**
         * Handle access denied errors from Spring Security
         * Returns HTTP 403 Forbidden
         */
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<CustomError> handleAccessDeniedException(
                        AccessDeniedException exception, WebRequest webRequest) {
                CustomError errorDetails = new CustomError(new Date(), "Access Denied: " + exception.getMessage(),
                                webRequest.getDescription(false));
                return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
        }

        /**
         * Handle illegal argument exceptions
         * Returns HTTP 400 Bad Request
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<CustomError> handleIllegalArgumentException(
                        IllegalArgumentException exception, WebRequest webRequest) {
                CustomError errorDetails = new CustomError(new Date(), exception.getMessage(),
                                webRequest.getDescription(false));
                return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle illegal state exceptions
         * Returns HTTP 409 Conflict
         */
        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<CustomError> handleIllegalStateException(
                        IllegalStateException exception, WebRequest webRequest) {
                CustomError errorDetails = new CustomError(new Date(), exception.getMessage(),
                                webRequest.getDescription(false));
                return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT);
        }

        /**
         * Handle custom exceptions with embedded status
         */
        @ExceptionHandler(CustomException.class)
        public ResponseEntity<CustomError> handleCustomException(
                        CustomException exception, WebRequest webRequest) {
                CustomError errorDetails = new CustomError(new Date(), exception.getMessage(),
                                webRequest.getDescription(false));
                return new ResponseEntity<>(errorDetails, exception.getHttpStatus());
        }

        /**
         * Handle all other exceptions
         * Returns HTTP 500 Internal Server Error
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<CustomError> handleGlobalException(Exception exception, WebRequest webRequest) {
                CustomError errorDetails = new CustomError(new Date(), exception.getMessage(),
                                webRequest.getDescription(false));
                return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
        }
}
