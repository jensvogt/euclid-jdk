package de.jensvogt.euclid.dto.eap.model;

/**
 * One application a worker node is holding slots for, as {@code get-node} reports it.
 *
 * <p>Two counts rather than one, because a node holding four slots and running none is exactly the
 * state worth seeing, and a single number cannot say it.
 *
 * @param applicationId the application's name, as it is managed under
 * @param runtimeName   the name euclid runs it under, which is what the module list calls the pool
 * @param namespace     the namespace the application belongs to
 * @param runtime       how its artifact is executed, e.g. {@code "JAVA"}
 * @param instances     slots this node holds for it
 * @param running       how many of those slots are actually serving
 */
public record NodeApplication(String applicationId, String runtimeName, String namespace, String runtime,
                              long instances, long running) {
}
