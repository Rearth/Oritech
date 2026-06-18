package rearth.oritech.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

public final class RegistryReflectionUtil {

    private RegistryReflectionUtil() {
    }

    // Custom functional interface to pass the Field, Identifier, and Value
    @FunctionalInterface
    public interface FieldConsumer<T> {
        void accept(Field field, String identifier, T value);
    }

    @SuppressWarnings("unchecked")
    public static <T> void ForEachPublicStaticField(Class<?> targetClass, Class<T> fieldType, FieldConsumer<T> action) {
        for (var field : targetClass.getDeclaredFields()) {
            int modifiers = field.getModifiers();

            if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) continue;
            if (!fieldType.isAssignableFrom(field.getType())) continue;

            try {
                field.setAccessible(true);
                T value = (T) field.get(null);
                var identifier = field.getName().toLowerCase(Locale.ROOT);

                // Pass the field itself so annotations can be read
                action.accept(field, identifier, value);

            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access static field: " + field.getName(), e);
            }
        }
    }
}