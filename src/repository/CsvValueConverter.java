package repository;

import java.lang.reflect.Field;

/** Converts a CSV token to the declared Java field type. */
final class CsvValueConverter {
    private CsvValueConverter() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static void assign(Object entity, Field field, String rawValue) throws IllegalAccessException {
        String value = rawValue.trim();
        Class<?> fieldType = field.getType();
        if (fieldType == String.class) {
            field.set(entity, value);
        } else if (fieldType == int.class || fieldType == Integer.class) {
            field.set(entity, value.isEmpty() ? 0 : Integer.parseInt(value));
        } else if (fieldType == long.class || fieldType == Long.class) {
            field.set(entity, value.isEmpty() ? 0L : Long.parseLong(value));
        } else if (fieldType == double.class || fieldType == Double.class) {
            field.set(entity, value.isEmpty() ? 0.0 : Double.parseDouble(value));
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            field.set(entity, Boolean.parseBoolean(value));
        } else if (fieldType.isEnum()) {
            field.set(entity, Enum.valueOf((Class<Enum>) fieldType, value.toUpperCase()));
        } else {
            throw new IllegalArgumentException("Unsupported CSV field type: " + fieldType.getName());
        }
    }
}
