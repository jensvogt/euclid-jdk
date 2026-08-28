package de.jensvogt.euclid.dto.ens.model;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::ENS::Topic} from the Euclid server.
 *
 * @param name             the topic's name
 * @param owner            the user ID that owns the topic
 * @param ern              the topic's ERN
 * @param tags             user-defined tags on the topic
 * @param size             total size in bytes of all messages currently in the topic
 * @param messages         number of messages currently in the topic
 * @param maxMessageLength maximum allowed size, in bytes, of a single message
 * @param created          creation timestamp
 * @param modified         last-modified timestamp
 */
public record Topic(String name, String owner, String ern, Map<String, String> tags, long size, long messages,
                     long maxMessageLength, String created, String modified) {
}
