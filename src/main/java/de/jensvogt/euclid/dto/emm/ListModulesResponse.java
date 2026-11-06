package de.jensvogt.euclid.dto.emm;

import de.jensvogt.euclid.dto.emm.model.ManagedModule;

import java.util.List;

/**
 * Response carrying every module known to the manager, as persisted in its module repository.
 *
 * @param modules the modules, each with its configuration, desired state and process pool
 * @param total   how many modules there are
 */
public record ListModulesResponse(List<ManagedModule> modules, long total) {
}
