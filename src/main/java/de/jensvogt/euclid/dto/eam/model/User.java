package de.jensvogt.euclid.dto.eam.model;

/**
 * Mirrors {@code Euclid::Dto::User} from the Euclid server.
 *
 * @param userId    the user's ID
 * @param password  the user's password
 * @param email     the user's email address
 * @param accountId ID of the account the user belongs to
 * @param region    the user's region
 * @param isAdmin   whether the user has administrator privileges
 */
public record User(String userId, String password, String email, String accountId, String region, boolean isAdmin) {
}
