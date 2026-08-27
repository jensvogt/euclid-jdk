package de.jensvogt.euclid.dto.eam.model;

import java.util.List;

/**
 * Mirrors {@code Euclid::Dto::UserGroup} from the Euclid server.
 *
 * @param name        group name
 * @param ern         the group's ERN
 * @param accountId   the account the group belongs to
 * @param region      the region the group was created in
 * @param description free-text description of the group's purpose
 * @param userIds     user IDs currently in this group
 * @param created     creation timestamp
 * @param modified    last-modified timestamp
 */
public record UserGroup(String name, String ern, String accountId, String region, String description,
                         List<String> userIds, String created, String modified) {
}
