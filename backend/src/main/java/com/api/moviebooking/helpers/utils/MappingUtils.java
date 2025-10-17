package com.api.moviebooking.helpers.utils;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class MappingUtils {

    public <T extends Enum<T>> Set<String> mapEnumToStrings(Set<T> enums) {
        if (enums == null)
            return null;
        return enums.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    public <T extends Enum<T>> String mapEnumToString(T enumValue) {
        return enumValue == null ? null : enumValue.name();
    }

    public String mapLocalDateTimeToString(LocalDateTime dateTime) {
        return dateTime == null ? null : DateTimeUtils.format(dateTime);
    }

    public String mapUUIDToString(UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }
}
