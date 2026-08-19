package de.jensvogt.euclid.dto.access.model;

/**
 * Mirrors {@code Euclid::Dto::User} from the Euclid server.
 */
public record User(String userId, String password, String email, String accountId, String region, boolean isAdmin) {
}
