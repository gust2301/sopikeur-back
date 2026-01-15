package sn.sopikeur.common.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class StringUtils {
    private StringUtils() {
    }

    public static List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .collect(Collectors.toList());
    }

    public static String joinCsv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .collect(Collectors.joining(","));
    }
}
