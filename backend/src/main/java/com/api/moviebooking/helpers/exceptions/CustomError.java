package com.api.moviebooking.helpers.exceptions;

import java.util.Date;

public record CustomError(Date timestamp, String message, String details) {
}
