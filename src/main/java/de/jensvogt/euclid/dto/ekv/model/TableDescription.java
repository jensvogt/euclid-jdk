package de.jensvogt.euclid.dto.ekv.model;

/**
 * Mirrors {@code Euclid::Dto::EKV::TableDescription} from the Euclid server: what a table looks
 * like, as an answer.
 *
 * <p>A table declares its key and nothing else. There is no schema for the rest of an item - two
 * items in the same table need have no attribute in common beyond the key - so what is here is the
 * key, the name it is reachable under, and how much is in it.
 *
 * @param partitionKey     the attribute every item is identified by
 * @param partitionKeyType the type that attribute has in every item
 * @param sortKey          the attribute items within a partition are ordered by, or {@code null} if
 *                         the table has none
 * @param sortKeyType      the type that attribute has, or {@code null} if the table has no sort key
 * @param name             the table's name, unique within the account, which every EKV action takes
 * @param ern              the table's ERN
 * @param itemCount        how many items the table holds. Counted when asked rather than kept in a
 *                         stored counter - a counter is a thing that drifts, and a count nobody can
 *                         trust is worse than one that costs a query
 * @param created          creation timestamp
 * @param modified         last-modified timestamp
 */
public record TableDescription(String name, String ern, String partitionKey, KeyType partitionKeyType,
                               String sortKey, KeyType sortKeyType, long itemCount, String created,
                               String modified) {

    /**
     * Whether items in this table are ordered within their partition, and so whether
     * {@link de.jensvogt.euclid.module.ekv.EuclidEkv#query} may narrow by anything but the
     * partition key.
     *
     * @return {@code true} if the table declares a sort key
     */
    public boolean hasSortKey() {
        return sortKey != null && !sortKey.isEmpty();
    }
}
