package de.jensvogt.euclid.dto.eam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.jensvogt.euclid.dto.eam.model.Role;

import java.util.List;

/**
 * The roles an account can bind.
 *
 * <p>Built-in roles come first and sit outside the paging, since they are not stored - which does
 * mean {@link #roles()} can hold more entries than {@link #total()} counts, because the total is
 * how many roles the <em>account</em> has defined.
 *
 * @param roles the built-in roles, then the account's own for the requested page
 * @param total how many roles the account has defined, across every page. Built-ins are not counted
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ListRolesResponse(List<Role> roles, long total) {
}
