package de.jensvogt.euclid;

import de.jensvogt.euclid.module.eam.EuclidEam;
import de.jensvogt.euclid.module.eqs.EuclidEqs;
import de.jensvogt.euclid.module.esm.EuclidEsm;

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

    public EuclidEam access() {
        return EuclidEam.forServer(baseUrl);
    }

    /**
     * Resolves an {@link EuclidEqs} using a cached session for this server (see
     * {@link EuclidEam#login()}). Requires a prior {@code access().credentials(...).login()}
     * call to have populated {@code ~/.euclid/credentials} with a still-valid token; otherwise
     * login() will fail requiring explicit credentials.
     */
    public EuclidEqs eqs() throws IOException, InterruptedException {
        return access().login().eqs();
    }

    /**
     * Resolves an {@link EuclidEsm} using a cached session for this server (see
     * {@link EuclidEam#login()}). Requires a prior {@code access().credentials(...).login()}
     * call to have populated {@code ~/.euclid/credentials} with a still-valid token; otherwise
     * login() will fail requiring explicit credentials.
     */
    public EuclidEsm esm() throws IOException, InterruptedException {
        return access().login().esm();
    }
}
