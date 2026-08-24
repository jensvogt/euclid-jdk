package de.jensvogt.euclid.dto.esm.model;

/**
 * Mirrors {@code Euclid::Dto::ESM::Object} from the Euclid server (named {@code EsmObject} here
 * rather than {@code Object} to avoid shadowing {@code java.lang.Object}).
 */
public record EsmObject(String ern, String bucketErn, String key, long size, String status, String contentType,
                         String md5Sum, String created, String modified) {
}
