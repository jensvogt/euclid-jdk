package de.jensvogt.euclid.dto.emm.model;

/**
 * One running slot of a module's process pool, as EMM's list-modules reports it.
 *
 * <p>The load fields - {@link #utilisation()}, {@link #backlog()} and {@link #loadReportedAt()} -
 * are what an instance writes about itself and the manager never observes. They are the whole of
 * what the autoscaler acts on for an application, which is why they are reported: without them,
 * "why is the pool not growing" has no answer short of reading the collection by hand.
 *
 * @param instanceId      the slot's identifier, which - unlike the pid - survives a restart
 * @param pid             the process ID, meaningful only together with {@code host}; -1 if none
 * @param host            the machine that pid is on, or empty on a single-host installation
 * @param state           the instance's state, e.g. {@code "RUNNING"}
 * @param socketPath      the Unix socket the instance serves on
 * @param httpPort        the port of the instance's own HTTP listener, 0 for a module with none
 * @param restartCount    how many times the manager has restarted this slot
 * @param utilisation     the load the instance last reported, or -1 if it has never reported
 * @param backlog         the work it last reported as waiting, or -1 if it has never reported
 * @param loadReportedAt  ISO8601 timestamp of its last load report, or empty if never
 * @param backgroundTasks work in progress no request is waiting on; scale-down passes over an
 *                        instance reporting any
 * @param started         ISO8601 timestamp the running process started, or empty if unknown
 * @param created         ISO8601 creation timestamp of the slot, which outlives every process in it
 * @param modified        ISO8601 last-modified timestamp
 */
public record ModuleInstance(String instanceId, long pid, String host, String state, String socketPath,
                             long httpPort, long restartCount, double utilisation, long backlog,
                             String loadReportedAt, long backgroundTasks, String started, String created,
                             String modified) {
}
