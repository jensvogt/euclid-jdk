package de.jensvogt.euclid.dto.ekv.model;

/**
 * How a query narrows the items within one partition by their sort key.
 * <p>
 * A query always names one partition exactly - that is what makes it a query rather than a scan -
 * and may then say which of that partition's items it wants. Every operator here is one the server
 * answers from the index, in sort-key order, which is the only reason a table declares a sort key
 * at all.
 */
public enum SortOperator {

    /**
     * No narrowing: the whole partition, in sort-key order. What a table without a sort key can
     * ask for, and the only thing it can.
     */
    NONE(""),

    /**
     * Sort key equal to the value.
     */
    EQ("eq"),

    /**
     * Sort key less than the value.
     */
    LT("lt"),

    /**
     * Sort key less than or equal to the value.
     */
    LE("le"),

    /**
     * Sort key greater than the value.
     */
    GT("gt"),

    /**
     * Sort key greater than the value, or equal to it.
     */
    GE("ge"),

    /**
     * Sort key between the value and the upper bound, both inclusive.
     */
    BETWEEN("between"),

    /**
     * Sort key starting with the value. The one operator here that is not a comparison, and the one
     * that only applies to a {@link KeyType#STRING} sort key - a prefix of a number is not a thing,
     * and the server refuses it rather than answering with nonsense.
     */
    BEGINS_WITH("begins-with");

    /**
     * The word the {@code ekv} module reads this operator as.
     */
    private final String wireValue;

    /**
     * Binds an operator to the word the API uses for it.
     *
     * @param wireValue the wire value
     */
    SortOperator(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * The wire value, as the {@code ekv} module reads it.
     *
     * @return "eq", "begins-with", and so on; empty for {@link #NONE}
     */
    public String wireValue() {
        return wireValue;
    }
}
