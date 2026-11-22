package de.jensvogt.euclid.dto.eap;

/**
 * The answer to assign-instance: which slot went where, and until when the claim holds.
 *
 * @param runtimeName    the pool the slot belongs to - the application's runtime name
 * @param instanceId     the slot that was assigned
 * @param node           the node it now belongs to, or empty if it was taken back to the manager's
 *                       own host
 * @param leaseExpiresAt when the claim lapses unless the node renews it (ISO 8601). A node that
 *                       does not renew in time loses the slot to placement, which is what stops a
 *                       switched-off machine holding work forever
 */
public record AssignInstanceResponse(String runtimeName, String instanceId, String node, String leaseExpiresAt) {
}
