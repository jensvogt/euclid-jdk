package de.jensvogt.euclid.dto.eap.model;

import java.util.List;
import java.util.Map;

/**
 * A registered worker node: a machine euclid may place application instances on.
 *
 * <p>A node registers itself and then renews a lease. {@link #live()} is derived from whether that
 * lease is still current rather than stored, so a machine that was switched off reads as not live
 * without anything having to notice and write it down.
 *
 * @param name         the node's name, unique within the account. Bound to {@link #principal()}:
 *                     the name is freed only by deleting the registration
 * @param address      where the manager reaches the node
 * @param principal    the user ID the node registered as, which is what owns the name
 * @param labels       what this machine carries, e.g. {@code os=windows}. An application's
 *                     {@code nodeLabels} have to be a subset of these for it to be placed here
 * @param cpuCount     how many CPUs the node reported
 * @param version      the worker version running on it
 * @param os           the operating system it reported
 * @param arch         the CPU architecture it reported
 * @param drained      whether the node is draining: refused <em>new</em> assignments while keeping
 *                     what it already has, which is the whole difference between draining and
 *                     stopping
 * @param live         whether the node's lease is still current. Derived, not stored
 * @param lastSeen     ISO8601 timestamp of its last renewal
 * @param applications what the node is running, which only
 *                     {@link de.jensvogt.euclid.module.eap.EuclidEap#getNode} reports - it is the
 *                     one thing about a node that is not on the node record, so a listing does not
 *                     pay for it. {@code null} from
 *                     {@link de.jensvogt.euclid.module.eap.EuclidEap#listNodes}
 */
public record Node(String name, String address, String principal, Map<String, String> labels, long cpuCount,
                   String version, String os, String arch, boolean drained, boolean live, String lastSeen,
                   List<NodeApplication> applications) {
}
