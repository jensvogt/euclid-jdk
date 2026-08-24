package de.jensvogt.euclid.exception;

/**
 * Thrown when the Euclid server rejects a login attempt.
 */
public class EuclidAuthenticationException extends RuntimeException {

    /**
     * The HTTP status code returned by the Euclid server when a login attempt is rejected.
     * This field provides information about the nature of the rejection, allowing for
     * further handling and debugging of authentication failures.
     */
    private final int statusCode;

    /**
     * Represents the body of the HTTP response returned by the Euclid server
     * when a login attempt is rejected. This field contains additional details
     * about the rejection, which can assist in understanding the cause of the
     * authentication failure.
     */
    private final String responseBody;

    /**
     * Constructs a new {@code EuclidAuthenticationException} with the specified HTTP status code
     * and response body returned by the Euclid server upon a failed login attempt.
     *
     * @param statusCode   the HTTP status code indicating the type of failure during the login attempt
     * @param responseBody the body of the HTTP response providing additional details about the failure
     */
    public EuclidAuthenticationException(int statusCode, String responseBody) {
        super("Login failed with status " + statusCode + ": " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * Retrieves the HTTP status code associated with the authentication failure
     * returned by the Euclid server. This status code indicates the type of
     * error that occurred during a login attempt.
     *
     * @return the HTTP status code representing the nature of the login failure
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Retrieves the body of the HTTP response returned by the Euclid server
     * when a login attempt is rejected. This provides additional details
     * about the reason for the rejection, which can aid in diagnosing
     * authentication issues.
     *
     * @return the HTTP response body containing details about the login failure
     */
    public String responseBody() {
        return responseBody;
    }
}
