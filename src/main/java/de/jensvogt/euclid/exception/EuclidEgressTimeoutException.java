package de.jensvogt.euclid.exception;

import java.io.IOException;
import java.time.Duration;

/**
 * No egress permit for a site came free within the time a caller was willing to wait.
 *
 * <p>An {@link IOException}, because to the caller it is one: the request it was about to make
 * cannot be made now, and whatever handles a failed download already handles this.
 */
public class EuclidEgressTimeoutException extends IOException {

    /**
     * The site no permit was granted for.
     */
    private final String site;

    /**
     * Creates the exception.
     *
     * @param site   the site no permit was granted for
     * @param waited how long the caller waited
     * @param reason why the last answer was a refusal: blocked, concurrency or rate
     */
    public EuclidEgressTimeoutException(String site, Duration waited, String reason) {
        super("no egress permit for " + site + " after " + waited.toSeconds() + "s (last refusal: " + reason + ")");
        this.site = site;
    }

    /**
     * The site no permit was granted for.
     *
     * @return the site
     */
    public String site() {
        return site;
    }
}
