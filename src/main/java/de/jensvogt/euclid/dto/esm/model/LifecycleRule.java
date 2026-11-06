package de.jensvogt.euclid.dto.esm.model;

/**
 * A bucket lifecycle rule: archive every object under {@code prefix} that has not been used for
 * {@code archiveAfterDays}.
 * <p>
 * An archived object keeps its key, attributes and listing entry, but its bytes move into an
 * archive and it has to be restored before it can be read. A restored object stays online for
 * {@code restoreDays} and is then archived again.
 *
 * @param id               the rule's name, unique within the bucket
 * @param prefix           the keys the rule covers; empty for the whole bucket
 * @param archiveAfterDays days without use after which an object is archived
 * @param restoreDays      days a restored object stays online before it is archived again
 * @param enabled          whether the rule is applied; a disabled one is kept but archives nothing
 */
public record LifecycleRule(String id, String prefix, long archiveAfterDays, long restoreDays, boolean enabled) {

    /**
     * An enabled rule.
     *
     * @param id               the rule's name, unique within the bucket
     * @param prefix           the keys the rule covers; empty or {@code null} for the whole bucket
     * @param archiveAfterDays days without use after which an object is archived
     * @param restoreDays      days a restored object stays online before it is archived again
     * @return the rule
     */
    public static LifecycleRule of(String id, String prefix, long archiveAfterDays, long restoreDays) {
        return new LifecycleRule(id, prefix == null ? "" : prefix, archiveAfterDays, restoreDays, true);
    }
}
