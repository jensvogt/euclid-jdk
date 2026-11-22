package de.jensvogt.euclid.dto.eds;

/**
 * Asks the name server a question - the body of {@code get-answer}.
 *
 * @param name the name to look up
 * @param type the type mnemonic; {@code null} or empty means {@code A}
 */
public record GetAnswerRequest(String name, String type) {
}
