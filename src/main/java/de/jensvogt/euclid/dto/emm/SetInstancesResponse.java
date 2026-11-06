package de.jensvogt.euclid.dto.emm;

/**
 * Response to set-instances: the limits now in force and the pool as it stands.
 *
 * @param name             the module
 * @param minInstances     the floor in force, whether set now or already standing
 * @param maxInstances     the ceiling in force, whether set now or already standing
 * @param runningInstances how many instances run right now; the manager moves the pool toward the
 *                         limits on its next reconcile
 */
public record SetInstancesResponse(String name, long minInstances, long maxInstances, long runningInstances) {
}
