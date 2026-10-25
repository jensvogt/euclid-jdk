package de.jensvogt.euclid.dto.eap;

/**
 * The answer to rebalance-application: the request was recorded, and how many instances there are
 * to spread. Nothing has moved yet when this is answered - the manager moves one instance per pass
 * until no move would help.
 *
 * @param applicationId the ID of the application
 * @param rebalancing   true once the request is recorded
 * @param instances     how many instances the application is running now
 */
public record RebalanceApplicationResponse(String applicationId, boolean rebalancing, long instances) {
}
