package io.github.snow1026.snowlib.utils.reflect;

import java.lang.reflect.Constructor;

/**
 * Utility class for simplifying Java Reflection operations.
 * <p>
 * Provides helper methods to dynamically locate classes and constructors
 * at runtime while ensuring accessibility.
 */
public class ReflectFinder {

    /**
     * Attempts to load a class by its fully-qualified name.
     *
     * @param path Fully-qualified class name (e.g., {@code java.util.ArrayList}).
     * @return The loaded {@link Class} instance, or {@code null} if not found.
     */
    public static Class<?> findClass(String path) {
        try {
            return Class.forName(path);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while loading class: " + path, e);
        }
    }

    /**
     * Finds a declared constructor of a class and makes it accessible.
     *
     * @param clazz          Target class.
     * @param parameterTypes Constructor parameter types.
     * @param <T>            Type of the target class.
     * @return An accessible constructor instance.
     * @throws RuntimeException If the constructor cannot be found or made accessible.
     */
    public static <T> Constructor<T> findConstructor(Class<T> clazz, Class<?>... parameterTypes) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while finding constructor of: " + clazz.getName(), e);
        }
    }
}
