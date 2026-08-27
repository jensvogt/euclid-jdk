package de.jensvogt.euclid.dto.eam.model;

/**
 * Mirrors {@code Euclid::Dto::Account} from the Euclid server.
 *
 * @param accountId   account ID, unique across the deployment
 * @param name        human-readable account name
 * @param ern         the account's ERN
 * @param description free-text description of the account's purpose
 * @param created     creation timestamp
 * @param modified    last-modified timestamp
 */
public record Account(String accountId, String name, String ern, String description, String created,
                       String modified) {
}
