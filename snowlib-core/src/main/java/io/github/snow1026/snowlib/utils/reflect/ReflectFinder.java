package io.github.snow1026.snowlib.utils.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A comprehensive utility class for simplifying and extending Java Reflection operations.
 * <p>
 * Provides a rich set of helper methods for all major reflection tasks,
 * designed to be robust and easy to use. This class handles exceptions by wrapping them
 * in {@link RuntimeException} to reduce boilerplate try-catch blocks in client code.
 *
 * <h3>Features:</h3>
 * <ul>
 * <li>Class, Constructor, Field, and Method finding.</li>
 * <li>Object instantiation and member invocation.</li>
 * <li>Analysis of class metadata (modifiers, hierarchy, interfaces).</li>
 * <li>Handling of Annotations, Generic Types, and Enums.</li>
 * <li>Dynamic creation and manipulation of Arrays and Proxies.</li>
 * </ul>
 *
 * @author Gemini AI
 * @version 1.0
 */
public final class ReflectFinder {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ReflectFinder() {
    }

    // ===================================================================================
    // Section 1: Core Finding & Instantiation
    // ===================================================================================

    /**
     * Attempts to load a class by its fully-qualified name.
     *
     * @param path The fully-qualified class name (e.g., {@code "java.util.ArrayList"}).
     * @return The loaded {@link Class} instance if found; otherwise, {@code null}.
     * @throws RuntimeException for unexpected errors during class loading, except for {@link ClassNotFoundException}.
     */
    public static Class<?> findClass(String path) {
        try {
            return Class.forName(path);
        } catch (ClassNotFoundException e) {
            return null; // As per design, return null if class is simply not found.
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while loading class: " + path, e);
        }
    }

    /**
     * Finds a declared constructor of a class and makes it accessible.
     * <p>
     * This method locates a constructor that matches the exact specified parameter types.
     * It allows access to non-public constructors.
     *
     * @param clazz          The target class.
     * @param parameterTypes The exact parameter types of the constructor to find.
     * @param <T>            The type of the target class.
     * @return An accessible {@link Constructor} instance.
     * @throws RuntimeException If the constructor cannot be found or an unexpected error occurs.
     */
    public static <T> Constructor<T> findConstructor(Class<T> clazz, Class<?>... parameterTypes) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            String paramsStr = Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(", "));
            throw new RuntimeException("Constructor not found in " + clazz.getName() + " with params: [" + paramsStr + "]", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while finding constructor of: " + clazz.getName(), e);
        }
    }

    /**
     * Creates a new instance of a class using its constructor and arguments.
     * <p>
     * This is a convenience method that combines finding a constructor and instantiating an object.
     * It infers parameter types from the arguments provided. Note that this can be ambiguous
     * if any argument is {@code null} or if primitive types are involved.
     *
     * @param clazz The class to instantiate.
     * @param args  The arguments to pass to the constructor.
     * @param <T>   The type of the object to create.
     * @return A new instance of the class.
     * @throws RuntimeException if the corresponding constructor is not found or if instantiation fails.
     */
    public static <T> T newInstance(Class<T> clazz, Object... args) {
        try {
            Class<?>[] parameterTypes = Arrays.stream(args).map(arg -> arg == null ? null : arg.getClass()).toArray(Class<?>[]::new);
            Constructor<T> constructor = findConstructor(clazz, parameterTypes);
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate " + clazz.getName(), e);
        }
    }

    /**
     * Finds a field in a class or its superclasses and makes it accessible.
     * <p>
     * This method searches the entire class hierarchy, starting from the given class
     * up to {@link Object}. It returns the first field found with the specified name,
     * making it accessible even if it is private or protected.
     *
     * @param clazz     The class to search in.
     * @param fieldName The name of the field to find.
     * @return The accessible {@link Field} instance.
     * @throws RuntimeException if the field is not found in the class or any of its superclasses.
     */
    public static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        if (clazz == null) return null;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass(); // Traverse up the hierarchy
            }
        }
        throw new RuntimeException("Field '" + fieldName + "' not found in class " + clazz.getName() + " or its superclasses.");
    }

    /**
     * Finds a method in a class or its superclasses and makes it accessible.
     * <p>
     * This method searches the entire class hierarchy for a method that matches the
     * specified name and parameter types. It makes the method accessible, allowing
     * invocation of non-public methods.
     *
     * @param clazz          The class to search in.
     * @param methodName     The name of the method to find.
     * @param parameterTypes The exact parameter types of the method.
     * @return The accessible {@link Method} instance.
     * @throws RuntimeException if the method is not found in the class or any of its superclasses.
     */
    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Class<?> current = clazz;
        if (clazz == null) return null;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass(); // Traverse up the hierarchy
            }
        }
        String paramsStr = Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(", "));
        throw new RuntimeException("Method '" + methodName + "(" + paramsStr + ")' not found in class " + clazz.getName() + " or its superclasses.");
    }

    // ===================================================================================
    // Section 2: Core Invocation & Manipulation
    // ===================================================================================

    /**
     * Gets the value of a field from an object instance.
     * <p>
     * This method can retrieve values from both instance and static fields.
     * For static fields, the {@code instance} parameter can be {@code null}.
     *
     * @param instance  The object instance to get the field value from (or {@code null} for a static field).
     * @param fieldName The name of the field.
     * @param <T>       The expected type of the field's value.
     * @return The value of the field, cast to the expected type.
     * @throws RuntimeException if the field is not found or if its value cannot be accessed.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object instance, String fieldName) {
        try {
            Class<?> clazz = (instance != null) ? instance.getClass() : null;
            if (clazz == null) {
                throw new IllegalArgumentException("Cannot determine class from a null instance.");
            }
            Field field = findField(clazz, fieldName);
            return (T) field.get(instance);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get value of field '" + fieldName + "'", e);
        }
    }

    /**
     * Sets the value of a field on an object instance.
     * <p>
     * This method can set values for both instance and static fields.
     * For static fields, the {@code instance} parameter can be {@code null}.
     *
     * @param instance  The object instance to set the field value on (or {@code null} for a static field).
     * @param fieldName The name of the field.
     * @param value     The new value for the field.
     * @throws RuntimeException if the field is not found or if the value cannot be set.
     */
    public static void setFieldValue(Object instance, String fieldName, Object value) {
        try {
            Class<?> clazz = (instance != null) ? instance.getClass() : null;
            if (clazz == null) {
                throw new IllegalArgumentException("Cannot determine class from a null instance.");
            }
            Field field = findField(clazz, fieldName);
            field.set(instance, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set value for field '" + fieldName + "'", e);
        }
    }

    /**
     * Invokes a method on an object instance with the given arguments.
     * <p>
     * This method handles both instance and static methods. For static methods,
     * the {@code instance} parameter can be {@code null}. Parameter types are
     * inferred from the arguments, which may cause issues with {@code null} values
     * or primitive types. For more precise control, use {@link #findMethod(Class, String, Class...)}
     * and {@link Method#invoke(Object, Object...)}.
     *
     * @param instance   The object instance to invoke the method on (or {@code null} for a static method).
     * @param methodName The name of the method to invoke.
     * @param args       The arguments to pass to the method.
     * @param <T>        The expected type of the method's return value.
     * @return The value returned by the method, cast to the expected type. Returns {@code null} if the method is {@code void}.
     * @throws RuntimeException if the method is not found or fails to be invoked.
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(Object instance, String methodName, Object... args) {
        try {
            Class<?> clazz = (instance != null) ? instance.getClass() : null;
            if (clazz == null) {
                throw new IllegalArgumentException("Cannot determine class from a null instance for method invocation.");
            }
            Class<?>[] parameterTypes = Arrays.stream(args).map(arg -> arg == null ? null : arg.getClass()).toArray(Class<?>[]::new);
            Method method = findMethod(clazz, methodName, parameterTypes);
            if (method == null) return null;
            return (T) method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke method '" + methodName + "'", e);
        }
    }

    // ===================================================================================
    // Section 3: Advanced Class Metadata Analysis
    // ===================================================================================

    /**
     * Returns the modifiers for a class (e.g., "public final") as a string.
     *
     * @param clazz The class to analyze.
     * @return A string representation of the class's modifiers.
     * @see Modifier
     */
    public static String getClassModifiers(Class<?> clazz) {
        return Modifier.toString(clazz.getModifiers());
    }

    /**
     * Checks if a class is a subclass or implementation of another.
     * <p>
     * This is equivalent to {@code parent.isAssignableFrom(child)}, which checks if an object
     * of type {@code child} can be legally assigned to a variable of type {@code parent}.
     *
     * @param child  The potential subclass or implementing class.
     * @param parent The potential superclass or interface.
     * @return {@code true} if {@code child} is a subtype of {@code parent}, {@code false} otherwise.
     */
    public static boolean isSubclassOf(Class<?> child, Class<?> parent) {
        return parent.isAssignableFrom(child);
    }

    /**
     * Gets all interfaces directly implemented by a class.
     *
     * @param clazz The class to analyze.
     * @return An array of {@link Class} objects representing the interfaces. Returns an empty array if none.
     */
    public static Class<?>[] getInterfaces(Class<?> clazz) {
        return clazz.getInterfaces();
    }

    /**
     * Gets the package name of a class.
     *
     * @param clazz The class to analyze.
     * @return The name of the package as a string.
     */
    public static String getPackageName(Class<?> clazz) {
        return clazz.getPackage().getName();
    }

    // ===================================================================================
    // Section 4: Annotation Handling
    // ===================================================================================

    /**
     * Gets a specific annotation from an annotated element.
     *
     * @param element         The element (a {@link Class}, {@link Method}, {@link Field}, etc.) to get the annotation from.
     * @param annotationClass The {@link Class} object corresponding to the annotation type.
     * @param <A>             The type of the annotation to retrieve.
     * @return The annotation instance for the specified type if present on the element, else {@code null}.
     */
    public static <A extends Annotation> A getAnnotation(AnnotatedElement element, Class<A> annotationClass) {
        return element.getAnnotation(annotationClass);
    }

    /**
     * Gets all annotations present on an annotated element.
     *
     * @param element The element (a {@link Class}, {@link Method}, {@link Field}, etc.) to get annotations from.
     * @return An array of all annotations. Returns an empty array if there are no annotations.
     */
    public static Annotation[] getAnnotations(AnnotatedElement element) {
        return element.getAnnotations();
    }

    /**
     * Checks if a specific annotation is present on an annotated element.
     *
     * @param element         The element (a {@link Class}, {@link Method}, {@link Field}, etc.) to check.
     * @param annotationClass The {@link Class} object corresponding to the annotation type.
     * @return {@code true} if the annotation is present, {@code false} otherwise.
     */
    public static boolean isAnnotationPresent(AnnotatedElement element, Class<? extends Annotation> annotationClass) {
        return element.isAnnotationPresent(annotationClass);
    }

    // ===================================================================================
    // Section 5: Enum & Generic Type Handling
    // ===================================================================================

    /**
     * Checks if a class is an Enum.
     *
     * @param clazz The class to check.
     * @return {@code true} if the class is an enum type, {@code false} otherwise.
     */
    public static boolean isEnum(Class<?> clazz) {
        return clazz.isEnum();
    }

    /**
     * Gets all constant values of an Enum class.
     *
     * @param enumClass The Enum class (e.g., {@code DayOfWeek.class}).
     * @param <E>       The type of the Enum.
     * @return An array containing all enum constants in the order they are declared.
     * @throws IllegalArgumentException if the provided class is not an enum type.
     */
    public static <E extends Enum<E>> E[] getEnumConstants(Class<E> enumClass) {
        if (!isEnum(enumClass)) {
            throw new IllegalArgumentException(enumClass.getName() + " is not an enum type.");
        }
        return enumClass.getEnumConstants();
    }

    /**
     * Gets the generic type of a field, preserving type parameters.
     * <p>
     * For example, for a field declared as {@code List<String> names;}, this method returns
     * a {@link Type} object representing {@code java.util.List<java.lang.String>}.
     *
     * @param field The field to analyze.
     * @return The {@link Type} object representing the generic type of the field.
     */
    public static Type getFieldGenericType(Field field) {
        return field.getGenericType();
    }

    /**
     * Gets the generic return type of a method, preserving type parameters.
     *
     * @param method The method to analyze.
     * @return The {@link Type} object representing the generic return type of the method.
     */
    public static Type getMethodGenericReturnType(Method method) {
        return method.getGenericReturnType();
    }

    // ===================================================================================
    // Section 6: Dynamic Array & Proxy Creation
    // ===================================================================================

    /**
     * Creates a new array instance of a specified component type and length.
     *
     * @param componentType The class of the array elements (e.g., {@code String.class}).
     * @param length        The length of the array to create.
     * @return A new array object (e.g., a {@code String[]}). This must be cast to the specific array type.
     */
    public static Object newArrayInstance(Class<?> componentType, int length) {
        return Array.newInstance(componentType, length);
    }

    /**
     * Gets the value from an array at a specific index using reflection.
     *
     * @param array The array object to get the value from.
     * @param index The index of the value to get.
     * @return The object at the specified index in the array.
     * @throws IllegalArgumentException if the provided object is not an array.
     */
    public static Object getArrayValue(Object array, int index) {
        return Array.get(array, index);
    }

    /**
     * Sets a value in an array at a specific index using reflection.
     *
     * @param array The array object to set the value in.
     * @param index The index at which to set the value.
     * @param value The new value to set.
     * @throws IllegalArgumentException if the provided object is not an array.
     */
    public static void setArrayValue(Object array, int index, Object value) {
        Array.set(array, index, value);
    }

    /**
     * Creates a dynamic proxy instance that implements a set of interfaces.
     * <p>
     * Any method call on the returned proxy will be dispatched to the {@code invoke}
     * method of the provided {@link InvocationHandler}. This is a powerful mechanism
     * for implementing cross-cutting concerns like logging, caching, or transactions.
     *
     * @param mainInterface   The primary interface for the proxy, used for type casting the result.
     * @param handler         The invocation handler to which method calls are dispatched.
     * @param otherInterfaces Optional additional interfaces for the proxy to implement.
     * @param <T>             The type of the primary interface.
     * @return A proxy instance that implements all the specified interfaces.
     * @see Proxy
     * @see InvocationHandler
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> mainInterface, InvocationHandler handler, Class<?>... otherInterfaces) {
        int interfaceCount = 1 + (otherInterfaces != null ? otherInterfaces.length : 0);
        Class<?>[] allInterfaces = new Class<?>[interfaceCount];
        allInterfaces[0] = mainInterface;
        if (otherInterfaces != null) {
            System.arraycopy(otherInterfaces, 0, allInterfaces, 1, otherInterfaces.length);
        }

        return (T) Proxy.newProxyInstance(
                mainInterface.getClassLoader(),
                allInterfaces,
                handler
        );
    }
}
