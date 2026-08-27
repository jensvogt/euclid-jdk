package de.jensvogt.euclid.dto.eam.model;

/**
 * Mirrors {@code Euclid::Dto::Namespace} from the Euclid server.
 *
 * @param accountId   the account this namespace belongs to
 * @param name        namespace name, unique within its account
 * @param ern         the namespace's ERN
 * @param description free-text description of the namespace's purpose
 * @param created     creation timestamp
 * @param modified    last-modified timestamp
 */
public record Namespace(String accountId, String name, String ern, String description, String created,
                         String modified) {
}
