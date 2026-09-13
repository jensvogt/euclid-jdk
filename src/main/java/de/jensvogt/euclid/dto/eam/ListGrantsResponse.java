package de.jensvogt.euclid.dto.eam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.jensvogt.euclid.dto.eam.model.Grant;

import java.util.List;

/**
 * The grants matching a principal, a role, or a whole account.
 *
 * @param grants the grants
 * @param total  how many there are
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ListGrantsResponse(List<Grant> grants, long total) {
}
