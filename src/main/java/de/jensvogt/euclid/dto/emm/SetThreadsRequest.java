package de.jensvogt.euclid.dto.emm;

/**
 * Request to set how many worker threads each of a module's processes serves requests with.
 *
 * @param name    the module, e.g. {@code "eqs"}
 * @param threads the number of worker threads, 1 to 256
 */
public record SetThreadsRequest(String name, int threads) {
}
