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

    private final String method;
    private final String target;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private String body = "";

    public SignableRequest(String method, String target) {
        this.method = method;
        this.target = target;
    }

    /**
     * Sets a header, overwriting any previous value. Names are matched case-insensitively, as
     * HTTP requires.
     */
    public SignableRequest header(String name, String value) {
        headers.put(name.toLowerCase(Locale.ROOT), value.trim());
        return this;
    }

    /**
     * @return the header's value, or {@code ""} if not set.
     */
    public String header(String name) {
        return headers.getOrDefault(name.toLowerCase(Locale.ROOT), "");
    }

    public SignableRequest body(String body) {
        this.body = body == null ? "" : body;
        return this;
    }

    public String method() {
        return method;
    }

    public String target() {
        return target;
    }

    public String body() {
        return body;
    }

    /**
     * @return all headers, keyed by lowercase name.
     */
    public Map<String, String> headers() {
        return Collections.unmodifiableMap(headers);
    }
}
