package de.jensvogt.euclid.dto.eam.model;

import java.util.List;

/**
 * Mirrors {@code Euclid::Dto::AccountGrant} from the Euclid server: an explicit grant of
 * per-(account, namespace) access held by a user, in addition to the user's home account.
 *
 * @param accountId  the account this grant applies to
 * @param namespaces namespaces within accountId this user may access
 * @param isAdmin    whether this user administers accountId itself
 * @param granted    timestamp the grant was created
 */
public record AccountGrant(String accountId, List<String> namespaces, boolean isAdmin, String granted) {
}
