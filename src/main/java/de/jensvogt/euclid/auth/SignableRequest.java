package de.jensvogt.euclid.auth;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Minimal, HTTP-client-agnostic view of a request that {@link SigV4} can sign or verify:
 * method, target (path plus optional {@code ?query}), headers and body.
 * <p>
 * Deliberately independent of {@code java.net.http.HttpRequest}, which is immutable and can't
 * have headers such as {@code Authorization} added to it after the fact the way {@link SigV4#sign}
 * needs to.
 */
public final class SignableRequest {

    /**
     * The HTTP method of the request (e.g., GET, POST, PUT, DELETE).
     * This value is immutable once set.
     */
    private final String method;

    /**
     * The target of the HTTP request, consisting of the path and optionally the query string.
     * This value is immutable once set and represents the full path component of the request's
     * URI, including any query parameters.
     */
    private final String target;

    /**
     * A collection of HTTP request headers associated with the signable request.
     *
     * The keys represent header names and are stored in a case-insensitive manner
     * (lowercased) to comply with the HTTP specification.
     * The values represent the corresponding header values, which are stored as strings.
     *
     * The headers are maintained in insertion order to ensure predictable header iteration.
     * This property is primarily used for adding, retrieving, or verifying headers
     * associated with the request during the signing or verification process.
     */
    private final Map<String, String> headers = new LinkedHashMap<>();

    /**
     * The body of the HTTP request.
     *
     * Represents the payload or content of the request, typically used in methods
     * such as POST or PUT where data is being sent to the server.
     * If no body is specified, the default value is an empty string.
     */
    private String body = "";

    /**
     * Constructs a new instance of the {@code SignableRequest} class with the specified HTTP method
     * and target URI.
     *
     * @param method the HTTP method of the request (e.g., GET, POST, PUT, DELETE). It must not be null.
     * @param target the target URI of the request, including the path and optionally the query string.
     *               It must not be null.
     */
    public SignableRequest(String method, String target) {
        this.method = method;
        this.target = target;
    }

    /**
     * Adds or updates a header to the request. The header name is case-insensitively stored
     * by converting it to lowercase. The header value is trimmed of leading and trailing spaces.
     *
     * @param name the name of the header. It must not be null.
     * @param value the value of the header. It must not be null.
     * @return the current instance of {@code SignableRequest} for method chaining.
     */
    public SignableRequest header(String name, String value) {
        headers.put(name.toLowerCase(Locale.ROOT), value.trim());
        return this;
    }

    /**
     * Retrieves the value of a specified header from the request. Header names are case-insensitive
     * and are internally stored in lowercase.
     *
     * @param name the name of the header whose value is to be retrieved. It must not be null.
     * @return the value of the specified header if it exists; otherwise, an empty string.
     */
    public String header(String name) {
        return headers.getOrDefault(name.toLowerCase(Locale.ROOT), "");
    }

    /**
     * Sets the body of the request. If the provided body is null, it defaults
     * to an empty string.
     *
     * @param body the body content to be included in the request. If null, it
     *             will be replaced with an empty string.
     * @return the current instance of {@code SignableRequest} for method chaining.
     */
    public SignableRequest body(String body) {
        this.body = body == null ? "" : body;
        return this;
    }

    /**
     * Retrieves the HTTP method of the request.
     *
     * @return the HTTP method as a string (e.g., "GET", "POST", "PUT", "DELETE").
     */
    public String method() {
        return method;
    }

    /**
     * Retrieves the target URI of the request, including the path and optionally the query string.
     *
     * @return the target URI as a string.
     */
    public String target() {
        return target;
    }

    /**
     * Retrieves the body content of the request.
     *
     * @return the body content of the request as a string.
     */
    public String body() {
        return body;
    }

    /**
     * Retrieves an unmodifiable view of the headers associated with the request.
     * The headers are stored as a map, where the keys represent the header names,
     * and the values represent the corresponding header values.
     *
     * @return an unmodifiable map of header name-value pairs. The keys are case-insensitive
     *         and stored in lowercase.
     */
    public Map<String, String> headers() {
        return Collections.unmodifiableMap(headers);
    }
}
