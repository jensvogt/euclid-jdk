package de.jensvogt.euclid.exception;

/**
 * Thrown when the Euclid server rejects a login attempt.
 */
public class EuclidAuthenticationException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public EuclidAuthenticationException(int statusCode, String responseBody) {
        super("Login failed with status " + statusCode + ": " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int statusCode() {
        return statusCode;
    }

    public String responseBody() {
        return responseBody;
    }
}
