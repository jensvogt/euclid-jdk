package de.jensvogt.euclid.dto.eam.model;

import java.util.List;

/**
 * Mirrors {@code Euclid::Dto::User} from the Euclid server.
 *
 * @param userId        the user's ID
 * @param ern           the user's ERN
 * @param password      the user's password
 * @param email         the user's email address
 * @param accountId     ID of the account the user belongs to (the user's home account)
 * @param region        the user's region
 * @param accountGrants explicit per-(account, namespace) grants held by this user, in addition
 *                      to accountId above
 * @param created       creation timestamp
 * @param modified      last-modified timestamp
 */
public record User(String userId, String ern, String password, String email, String accountId, String region,
                    List<AccountGrant> accountGrants, String created, String modified) {
}
