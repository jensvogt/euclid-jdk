package de.jensvogt.euclid.dto.eam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * One role, given to one principal, somewhere.
 *
 * <p>The only thing that grants anything, and the only thing that carries scope. Replaced the
 * per-user {@code accountGrants}/{@code resourceGrants} lists: what a user may do is the union of
 * the grants held by them and by every group they belong to.
 *
 * @param grantId    the grant's own id, which is what revoking takes - not the (role, principal)
 *                   pair, since the same role may be granted to the same principal twice with
 *                   different scope and revoking has to say which
 * @param role       the role granted: one of the account's own, or a built-in
 * @param principal  who holds it: a user ERN or a user-group ERN, the ERN saying which
 * @param accountId  the account it applies in
 * @param namespaces namespaces of that account it applies in; a single "*" means all of them
 * @param resources  ERN patterns it applies to, each exact or ending in "*"; a single "*" means all
 * @param granted    when it was granted
 * @param grantedBy  the user ID that granted it
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Grant(String grantId, String role, String principal, String accountId, List<String> namespaces,
                    List<String> resources, String granted, String grantedBy) {
}
