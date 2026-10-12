package de.jensvogt.euclid.dto.eap;

import de.jensvogt.euclid.dto.eap.model.Node;

import java.util.List;

/**
 * The worker nodes registered to this account, and whether they are live.
 *
 * <p>Every node, not a page of them: a registration is a machine somebody set up, so an
 * installation with enough of them for a page boundary to matter is not one this listing is the
 * problem for.
 *
 * @param nodes the registered nodes, each without its {@code applications} - see
 *              {@link de.jensvogt.euclid.module.eap.EuclidEap#getNode} for what one is running
 * @param total how many there are
 */
public record ListNodesResponse(List<Node> nodes, long total) {
}
