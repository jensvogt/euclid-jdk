package de.jensvogt.euclid.dto.eam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * What {@code list-permissions} answers: every permission a role can hold, and the modules they
 * come from.
 * <p>
 * Generated server-side from the modules' own dispatch tables, so it is exactly what can be
 * granted. The modules that are not bindable at all contribute nothing to {@code permissions} and
 * are named separately, so a caller can say why they are missing rather than wondering.
 *
 * @param permissions       every permission, sorted, as {@code "<module>:<action>"}
 * @param modules           the modules that have permissions, sorted - what a {@code "<module>:*"}
 *                          permission may name
 * @param unbindableModules the modules whose actions can never be granted to anybody - today
 *                          {@code emd} and {@code emm}. Only an installation administrator reaches
 *                          them
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ListPermissionsResponse(List<String> permissions, List<String> modules, List<String> unbindableModules) {
}
