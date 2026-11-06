package de.jensvogt.euclid.dto.emm.model;

import java.util.List;

/**
 * A module known to the manager, with its configuration, its desired state and its current process
 * pool - one entry of EMM's list-modules.
 *
 * <p>Named {@code ManagedModule} rather than {@code Module} so that it does not shadow
 * {@link java.lang.Module} wherever it is imported.
 *
 * <p>The {@code desired*} fields are limits set through set-instances and set-threads but not yet
 * reconciled into the live ones; -1 means nothing is pending. A caller adjusting a limit twice in
 * quick succession reads these, or it computes the second change from a value the first has
 * already replaced.
 *
 * @param name                the module's name, e.g. {@code "esm"}
 * @param executable          the executable the manager starts for it
 * @param socketPath          the socket path its instances' sockets are derived from
 * @param active              whether the module is active
 * @param core                whether it is one of euclid's own modules; only those can be stopped
 *                            and started through EMM - applications and transfer servers have their
 *                            desired state in EAP and ETS
 * @param desiredStopped      whether it was stopped with stop-module and is kept stopped
 * @param autoRestart         whether the manager restarts an instance that exits
 * @param maxRestarts         how many restarts the manager makes before giving up, -1 for no limit
 * @param minInstances        the floor of instances kept running
 * @param maxInstances        the ceiling the autoscaler may scale up to
 * @param desiredMinInstances a floor set but not yet reconciled, or -1
 * @param desiredMaxInstances a ceiling set but not yet reconciled, or -1
 * @param desiredThreads      a worker-thread count set but not yet applied, or -1
 * @param logLevel            the level the module's output is logged at by the manager, or empty
 *                            for the configured default
 * @param created             ISO8601 creation timestamp
 * @param modified            ISO8601 last-modified timestamp
 * @param lastStartTime       ISO8601 timestamp of the last start, or {@code null} if never started
 * @param instances           the module's current process pool
 */
public record ManagedModule(String name, String executable, String socketPath, boolean active, boolean core,
                            boolean desiredStopped, boolean autoRestart, long maxRestarts, long minInstances,
                            long maxInstances, long desiredMinInstances, long desiredMaxInstances,
                            long desiredThreads, String logLevel, String created, String modified,
                            String lastStartTime, List<ModuleInstance> instances) {
}
