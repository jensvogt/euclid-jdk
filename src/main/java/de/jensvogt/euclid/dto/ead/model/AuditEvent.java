package de.jensvogt.euclid.dto.ead.model;

/**
 * Mirrors one entry of EAD's audit trail ({@code Euclid::Database::Entity::EAD::AuditEvent} on the
 * Euclid server): one command somebody ran, and who they were when they ran it.
 *
 * <p>{@link #status()} is what makes this an audit rather than a log of activity. The entry an
 * audit is usually opened to find is not the delete that worked but the one that was refused - a
 * 403, which is otherwise indistinguishable from a delete nobody attempted.
 *
 * <p>{@link #parameters()} is redacted before it is ever stored: sensitive values (passwords,
 * secrets, private keys) are replaced with {@code ***}, so a credential is never in the trail to be
 * read here.
 *
 * @param accountId  the account the command was run in
 * @param namespace  the namespace the command was run in, or empty for a command that names none
 * @param userId     who ran it, as the verified caller identity rather than whatever the body claimed
 * @param module     the module the command was addressed to, e.g. {@code "esm"}
 * @param command    the command, e.g. {@code "delete-bucket"}
 * @param parameters what it was called with, as a string, with sensitive values replaced
 * @param status     the HTTP status the command was answered with
 * @param created    ISO8601 timestamp of when it happened
 */
public record AuditEvent(String accountId, String namespace, String userId, String module, String command,
                         String parameters, long status, String created) {
}
