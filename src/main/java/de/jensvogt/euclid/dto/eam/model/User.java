package de.jensvogt.euclid.dto.eam.model;

/**
 * Mirrors {@code Euclid::Dto::User} from the Euclid server.
 */
public record User(String userId, String password, String email, String accountId, String region, boolean isAdmin) {
}
