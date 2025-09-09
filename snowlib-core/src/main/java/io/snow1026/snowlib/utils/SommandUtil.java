package io.snow1026.snowlib.utils;

/**
 * Utility functions (currently placeholder for future use)
 */
public class SommandUtil {

    public static String join(String[] arr, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            if (i > start) sb.append(" ");
            sb.append(arr[i]);
        }
        return sb.toString();
    }
}
