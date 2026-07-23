package repository;

import model.BaseEntity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reflection mapping between one entity and one CSV row. */
final class CsvReflectionMapper<T extends BaseEntity> {
    private final Class<T> type;
    private final String[] columns;
    private final Map<String, Field> fieldsByColumn;

    CsvReflectionMapper(Class<T> type, String header) {
        this.type = type;
        this.columns = header.split(",");
        this.fieldsByColumn = resolveFields();
    }

    T parse(String line, String sourceName) {
        String[] values = CsvRowCodec.split(line);
        try {
            T entity = type.getDeclaredConstructor().newInstance();
            for (int index = 0; index < Math.min(columns.length, values.length); index++) {
                Field field = fieldsByColumn.get(columns[index].trim());
                if (field != null) {
                    CsvValueConverter.assign(entity, field, values[index]);
                }
            }
            return entity;
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid CSV record in " + sourceName + ": " + line, exception);
        }
    }

    String serialize(T entity) {
        List<String> values = new ArrayList<>(columns.length);
        for (String rawColumn : columns) {
            Field field = fieldsByColumn.get(rawColumn.trim());
            try {
                Object value = field == null ? null : field.get(entity);
                values.add(CsvRowCodec.escape(value == null ? "" : value.toString()));
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Cannot serialize field " + rawColumn, exception);
            }
        }
        return String.join(",", values);
    }

    Integer readOptionalVersion(T entity) {
        Field version = fieldsByColumn.get("version");
        if (version == null) {
            return null;
        }
        try {
            return version.getInt(entity);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read version", exception);
        }
    }

    private Map<String, Field> resolveFields() {
        Map<String, Field> resolved = new LinkedHashMap<>();
        for (String rawColumn : columns) {
            String column = rawColumn.trim();
            Field field = findField(column);
            if (field != null) {
                field.setAccessible(true);
                resolved.put(column, field);
            }
        }
        return resolved;
    }

    private Field findField(String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
