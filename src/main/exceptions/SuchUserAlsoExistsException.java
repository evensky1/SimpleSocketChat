package main.exceptions;

public class SuchUserAlsoExistsException extends Exception {
    String message;

    public SuchUserAlsoExistsException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
