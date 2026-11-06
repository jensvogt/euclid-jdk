package de.jensvogt.euclid.dto.emm;

/**
 * Response to set-threads.
 *
 * @param name             the module
 * @param threads          the worker-thread count recorded
 * @param runningInstances how many instances the manager still has to cycle through before every
 *                         one runs with it - a thread count is fixed when a process starts
 */
public record SetThreadsResponse(String name, long threads, long runningInstances) {
}
