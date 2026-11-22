package de.jensvogt.euclid.dto.eds.model;

import java.util.List;

/**
 * What the name server answers for a question, as {@code get-answer} reports it - the same answer a
 * DNS client gets, without needing one.
 *
 * <p>A name in none of EDS's zones comes back {@code REFUSED} and not {@link #authoritative()}: the
 * DNS port would forward it when forwarders are configured, but {@code get-answer} only shows what
 * EDS knows itself. A name that does not exist in a zone comes back {@code NXDOMAIN}, and one that
 * exists without records of the type asked for comes back {@code NOERROR} with no answers - in both
 * cases with the zone's SOA in {@link #authority()}.
 *
 * @param name          the name asked for, as it was given
 * @param type          the type asked for
 * @param authoritative whether the name is in one of EDS's zones
 * @param zone          the apex of the zone that answered, or {@code null} when none did
 * @param rcode         the response code: {@code NOERROR}, {@code NXDOMAIN}, {@code SERVFAIL} or {@code REFUSED}
 * @param answers       the answer section, CNAMEs followed included
 * @param authority     the authority section - the zone's SOA on a negative answer
 */
public record Answer(String name, String type, boolean authoritative, String zone, String rcode,
                     List<AnswerRecord> answers, List<AnswerRecord> authority) {

    /**
     * Copies the sections so the record stays immutable.
     */
    public Answer {
        answers = answers == null ? List.of() : List.copyOf(answers);
        authority = authority == null ? List.of() : List.copyOf(authority);
    }
}
