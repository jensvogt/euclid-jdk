package de.jensvogt.euclid.dto.ekv.model;

/**
 * What a table's partition or sort key attribute may be.
 * <p>
 * Fewer types than an attribute value can hold, deliberately: a key is compared and ordered, and
 * these are the ones an ordering means something for. The type is fixed when the table is created
 * and every item has to honour it, which is what makes a range query mean what it should - a
 * {@link #NUMBER} sort key orders 2, 9, 10, 100 rather than putting "10" before "9".
 */
public enum KeyType {

    /**
     * Text, ordered lexicographically. The default, and the only type
     * {@link SortOperator#BEGINS_WITH} applies to.
     */
    STRING,

    /**
     * A number, ordered numerically.
     */
    NUMBER,

    /**
     * Raw bytes, ordered by their contents. Nothing arriving over the wire can be one today - JSON
     * has no spelling for bytes - so a binary key can be declared but not yet written from here.
     */
    BINARY;

    /**
     * The wire value, as the {@code ekv} module reads and reports it.
     *
     * @return "string", "number" or "binary"
     */
    public String wireValue() {
        return name().toLowerCase();
    }

    /**
     * Reads a wire value back.
     * <p>
     * Answers {@code null} for an absent or unrecognized one rather than guessing, because the
     * table that has no sort key reports an empty sort key type - and "this table is not sorted" is
     * not the same statement as "this table is sorted by a string".
     *
     * @param value the wire value, e.g. "number"
     * @return the type it names, or {@code null} if it names none
     */
    public static KeyType fromWireValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (KeyType type : values()) {
            if (type.wireValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
