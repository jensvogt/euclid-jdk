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
 * @param memoryMb     memory the node offers to applications, in MiB, as it registered - less than
 *                     {@code memoryTotalMb}, which is the machine; 0 if it did not say
 * @param memoryTotalMb the machine's installed memory in MiB, the "total" of "used / total"; 0 for
 *                     a worker too old to report it
 * @param memoryUsedMb how much of that was in use at the last renewal, in MiB; 0 if not reported
 * @param loadAverage  the one-minute load average from the node's last heartbeat, exactly as
 *                     current as {@code lastSeen}. Zero both for an idle machine and for a
 *                     heartbeat that carried no reading - placement does not tell them apart
 *                     either, and reads absent as idle on purpose. On the record rather than only a
 *                     monitoring gauge because a node sharing the manager's host publishes no
 *                     gauges at all
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
                   long memoryMb, long memoryTotalMb, long memoryUsedMb, double loadAverage, String version, String os, String arch, boolean drained, boolean live, String lastSeen,
                   List<NodeApplication> applications) {
}
