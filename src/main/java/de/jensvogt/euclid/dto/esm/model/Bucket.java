package de.jensvogt.euclid.dto.esm.model;

/**
 * Mirrors {@code Euclid::Dto::ESM::Bucket} from the Euclid server.
 */
public record Bucket(String region, String owner, String name, String ern, long size, long objects, String created,
                      String modified) {
}
