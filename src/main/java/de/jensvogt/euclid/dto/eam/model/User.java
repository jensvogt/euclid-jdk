package de.jensvogt.euclid.dto.eam.model;

/**
 * Mirrors {@code Euclid::Dto::User} from the Euclid server.
 *
 * @param userId        the user's ID
 * @param ern           the user's ERN
 * @param password      the user's password
 * @param email         the user's email address
 * @param accountId     ID of the account the user belongs to (the user's home account)
 * @param region        the user's region
 * @param created       creation timestamp
 * @param modified      last-modified timestamp
 */
public record User(String userId, String ern, String password, String email, String accountId, String region,
                    String created, String modified) {
}
