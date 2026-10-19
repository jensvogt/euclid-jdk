package de.jensvogt.euclid.dto.eap.model;

/**
 * What kind of thing an application is, and therefore what finishing means.
 *
 * <p>Everything euclid ran before this existed was a {@link #PROCESS}: started, expected to stay
 * up, and an exit is a fault to be restarted from. For something that is <em>supposed</em> to end
 * that is wrong twice over - a nightly import that completed would be restarted forever, and each
 * run reported as a crash.
 */
public enum ApplicationType {

    /**
     * Started and kept up. An exit is a fault, the pool is held at its instance count, and the
     * autoscaler grows and shrinks it with the backlog.
     */
    PROCESS,

    /**
     * Started to do one thing and finish. Exit 0 is success and not a restart, the slot is not
     * re-placed, and the autoscaler leaves it alone - a backlog says nothing about how many copies
     * of a one-shot task to run. A job may also carry a {@code schedule} to run on.
     */
    JOB,

    /**
     * What an unrecognised value reads as - a definition written by a newer euclid, or a document
     * somebody edited. The server treats it as {@link #PROCESS} wherever behaviour is decided, so
     * such a definition keeps the long-running behaviour every application had before types
     * existed. Never send it: the server refuses anything but PROCESS or JOB with HTTP 400.
     */
    UNKNOWN;

    /**
     * The wire value, as the {@code eap} module reads and reports it.
     *
     * @return "PROCESS", "JOB" or "UNKNOWN"
     */
    public String wireValue() {
        return name();
    }

    /**
     * Reads a wire value back.
     * <p>
     * Answers {@link #UNKNOWN} for anything unrecognised rather than falling back to
     * {@link #PROCESS}, so a caller can tell "this is a long-running application" from "this is
     * something this client does not understand". Note the server is case-sensitive here -
     * {@code "job"} is refused rather than read as {@link #JOB}, deliberately, so a typo cannot
     * quietly turn a service into a one-shot task.
     *
     * @param value the wire value, e.g. "JOB"
     * @return the type it names, or {@link #UNKNOWN} if it names none
     */
    public static ApplicationType fromWireValue(String value) {
        if (value == null || value.isEmpty()) {
            return UNKNOWN;
        }
        for (ApplicationType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * Whether this type runs to completion, which is the only question most callers ask.
     *
     * <p>Written as "is it a JOB" rather than "is it not a PROCESS" so that {@link #UNKNOWN} keeps
     * the long-running behaviour, matching what the server decides.
     *
     * @return {@code true} only for {@link #JOB}
     */
    public boolean isJob() {
        return this == JOB;
    }
}
