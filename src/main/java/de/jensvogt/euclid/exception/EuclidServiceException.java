package de.jensvogt.euclid.exception;

/**
 * Thrown when a Euclid service answers an action with a non-2xx status.
 * <p>
 * This is the general failure every module action reports: a missing bucket, a malformed request,
 * a queue that no longer exists, an internal server error. The service and action that failed are
 * carried alongside the status and body so a failure identifies itself without the caller having to
 * reconstruct it from a stack trace - {@code "esm create-upload failed with status 500: ..."} rather
 * than a bare status code.
 * <p>
 * {@link EuclidAuthenticationException} is the one specialization, reserved for a rejected login.
 * Catching this type therefore catches an authentication failure too, while catching that subtype
 * catches only a failed login.
 */
public class EuclidServiceException extends RuntimeException {

    /**
     * The Euclid service the failed action was addressed to, e.g. {@code "esm"} - the same value
     * that was sent as the request's {@code x-euclid-target} header.
     */
    private final String service;

    /**
     * The action that failed, e.g. {@code "create-upload"} - the same value that was sent as the
     * request's {@code x-euclid-action} header.
     */
    private final String action;

    /**
     * The HTTP status code returned by the Euclid server. Distinguishes a request the server
     * refused (4xx) from one it failed to serve (5xx), which is what makes a failure worth
     * retrying or not.
     */
    private final int statusCode;

    /**
     * The body of the HTTP response, usually a JSON object carrying an {@code "error"} field with
     * the server's own description of what went wrong.
     */
    private final String responseBody;

    /**
     * Constructs a new {@code EuclidServiceException} for an action a Euclid service rejected.
     *
     * @param service      the service the action was addressed to, e.g. {@code "esm"}
     * @param action       the action that failed, e.g. {@code "create-upload"}
     * @param statusCode   the HTTP status code returned by the Euclid server
     * @param responseBody the body of the HTTP response, describing what went wrong
     */
    public EuclidServiceException(String service, String action, int statusCode, String responseBody) {
        this(service + " " + action + " failed with status " + statusCode + ": " + responseBody,
                service, action, statusCode, responseBody);
    }

    /**
     * Constructs a new {@code EuclidServiceException} with a caller-supplied message, for
     * subclasses that describe their failure in their own terms rather than as a generic action
     * failure.
     *
     * @param message      the detail message describing the failure
     * @param service      the service the action was addressed to, e.g. {@code "eam"}
     * @param action       the action that failed, e.g. {@code "login"}
     * @param statusCode   the HTTP status code returned by the Euclid server
     * @param responseBody the body of the HTTP response, describing what went wrong
     */
    protected EuclidServiceException(String message, String service, String action, int statusCode, String responseBody) {
        super(message);
        this.service = service;
        this.action = action;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * Retrieves the Euclid service the failed action was addressed to.
     *
     * @return the service name, e.g. {@code "esm"}
     */
    public String service() {
        return service;
    }

    /**
     * Retrieves the action that failed.
     *
     * @return the action name, e.g. {@code "create-upload"}
     */
    public String action() {
        return action;
    }

    /**
     * Retrieves the HTTP status code the Euclid server returned for the failed action.
     *
     * @return the HTTP status code representing the nature of the failure
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Retrieves the body of the HTTP response the Euclid server returned for the failed action.
     *
     * @return the HTTP response body containing details about the failure
     */
    public String responseBody() {
        return responseBody;
    }
}
