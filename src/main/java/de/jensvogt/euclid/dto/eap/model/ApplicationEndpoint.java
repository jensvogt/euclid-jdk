package de.jensvogt.euclid.dto.eap.model;

/**
 * One running instance of an application, and the port it answers on.
 *
 * <p>Where the pool actually is. Ports are handed out at spawn time and change as the pool scales,
 * so they cannot be configured anywhere - an API gateway in front of an application, euclid's own
 * (see {@link de.jensvogt.euclid.module.eag.EuclidEag}) or an external one, discovers its backends
 * from exactly this.
 *
 * <p>Only instances the manager observes as RUNNING are reported, so an application that is
 * starting, stopped or failing has fewer endpoints than {@code minInstances} - or none.
 *
 * @param instanceId the manager's ID for this instance
 * @param pid        the operating system process ID, or {@code -1} if the manager has not recorded one
 * @param httpPort   the port this instance serves on
 */
public record ApplicationEndpoint(String instanceId, int pid, int httpPort) {
}
