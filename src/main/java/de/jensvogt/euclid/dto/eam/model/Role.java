package de.jensvogt.euclid.dto.eam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A named set of permissions, which a {@link Grant} binds to a principal somewhere.
 *
 * <p>A role says <em>what</em> may be done and carries no scope of its own - where and to which
 * resources is the grant's business. That split is what lets one {@code reader} role be granted in
 * one namespace to one user and across every namespace to another.
 *
 * @param name        the role's name, unique within the account. A grant names a role by this
 * @param ern         the role's ERN
 * @param accountId   the account the role belongs to
 * @param region      the region the role belongs to
 * @param description what the role is for
 * @param permissions the permissions it carries, each one a module answers - see
 *                    {@link de.jensvogt.euclid.module.eam.EuclidSession#listPermissions()}
 * @param builtin     whether euclid ships this role rather than the account having defined it.
 *                    A built-in cannot be created, changed or deleted, and a stored role may not
 *                    take its name: a grant resolves the account's own roles first, so one that
 *                    shadowed a built-in would silently replace it for that account alone
 * @param created     creation timestamp, empty for a built-in
 * @param modified    last-modified timestamp, empty for a built-in
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Role(String name, String ern, String accountId, String region, String description,
                   List<String> permissions, boolean builtin, String created, String modified) {
}
