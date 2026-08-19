package de.jensvogt.euclid;

import de.jensvogt.euclid.module.access.EuclidAccess;
import de.jensvogt.euclid.module.sqs.EuclidSqs;

import java.io.IOException;
import java.util.Objects;

/**
 * Main entry point for talking to an Euclid server, e.g.
 * {@code Euclid.forServer(url).access().credentials(...).login()}.
 */
public final class Euclid {

    private final String baseUrl;

    private Euclid(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static Euclid forServer(String baseUrl) {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        return new Euclid(baseUrl);
    }

    public EuclidAccess access() {
        return EuclidAccess.forServer(baseUrl);
    }

    /**
     * Resolves an {@link EuclidSqs} using a cached session for this server (see
     * {@link EuclidAccess#login()}). Requires a prior {@code access().credentials(...).login()}
     * call to have populated {@code ~/.euclid/credentials} with a still-valid token; otherwise
     * login() will fail requiring explicit credentials.
     */
    public EuclidSqs sqs() throws IOException, InterruptedException {
        return access().login().sqs();
    }
}
