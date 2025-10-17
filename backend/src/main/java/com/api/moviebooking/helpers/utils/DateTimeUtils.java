package com.api.moviebooking.helpers.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null)
            return null;
        return dateTime.format(FORMATTER);
    }
}
