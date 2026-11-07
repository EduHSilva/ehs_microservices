package com.edu.silva.users.services.impl;

import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

final class CsvUtils {

    static final String BOM = "\uFEFF";
    private static final String SEPARATOR = ";";

    private CsvUtils() {
    }

    static String escape(Object value) {
        if (value == null) {
            return "";
        }
        return "\"" + String.valueOf(value).replace("\"", "\"\"") + "\"";
    }

    static String date(Date date) {
        return date == null ? "" : date.toInstant().toString();
    }

    static String join(Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }

    static String line(Object... values) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                line.append(SEPARATOR);
            }
            line.append(escape(values[i]));
        }
        return line.append('\n').toString();
    }
}
