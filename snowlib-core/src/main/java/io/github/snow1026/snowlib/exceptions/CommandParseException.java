package io.github.snow1026.snowlib.exceptions;

/**
 * An exception thrown when there is an error during the parsing or execution of a command.
 * <p>
 * This could be due to invalid arguments, insufficient permissions, or unmet requirements.
 */
public class CommandParseException extends RuntimeException {

    public CommandParseException(String message) {
        super(message);
    }

    public CommandParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
