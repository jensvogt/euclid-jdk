package de.jensvogt.euclid.exception;

/**
 * Thrown when the Euclid server rejects a login attempt.
 * <p>
 * Reserved for the login itself. Every other action reports a non-2xx response as a plain
 * {@link EuclidServiceException}, which this extends - a failed bucket creation or queue read is
 * not an authentication failure, and reporting it as one only obscures what the server actually
 * said.
 */
public class EuclidAuthenticationException extends EuclidServiceException {

    /**
     * Constructs a new {@code EuclidAuthenticationException} with the specified HTTP status code
     * and response body returned by the Euclid server upon a failed login attempt.
     *
     * @param statusCode   the HTTP status code indicating the type of failure during the login attempt
     * @param responseBody the body of the HTTP response providing additional details about the failure
     */
    public EuclidAuthenticationException(int statusCode, String responseBody) {
        super("Login failed with status " + statusCode + ": " + responseBody, "eam", "login", statusCode, responseBody);
    }
}
