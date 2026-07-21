package repository;

import java.util.ArrayList;
import java.util.List;

/** RFC-4180-style escaping for the subset used by this project. */
public final class CsvRowCodec {
    private CsvRowCodec() {
    }

    static String[] split(String line) {
        List<String> values = new ArrayList<>();
        boolean quoted = false;
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unclosed CSV quote");
        }
        values.add(current.toString());
        return values.toArray(new String[0]);
    }

    public static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
