package de.jensvogt.euclid.dto.eag.model;

import java.util.List;

/**
 * Where an {@link RouteType#UPLOAD} route puts what it receives, and what it will accept - the
 * {@code upload} object of a route as the {@code eag} module reports it.
 * <p>
 * A document of its own on the server rather than five more fields on the route, because it only
 * means something on an upload route: a proxy route carrying an empty bucket and a zero part size
 * would invite the question of what they mean, and the answer is nothing. The server reports it on
 * every route regardless, empty on a proxy route.
 *
 * @param bucket       ERN of the bucket bodies are written to. An ERN rather than a name, because a
 *                     route may publish into a namespace other than the one it is managed from, and
 *                     a bare name would be resolved in the wrong one
 * @param keyPrefix    prefix every key is confined to, or empty for the whole bucket. The home
 *                     directory of a transfer server, in a bucket: without it anybody who may upload
 *                     at all may overwrite any key the bucket holds, since ESM's grants are per
 *                     bucket and "may add but not replace" is not something they can say
 * @param maxBytes     largest body the route accepts, in bytes, or 0 for no limit beyond the
 *                     listener's. Per route because it is a property of what is published: a route
 *                     taking catalogue deliveries and one taking profile pictures have nothing to say
 *                     to each other about size. Refused before the body is read rather than after
 * @param partSize     bytes per part streamed to ESM, and the size under which a body is stored with
 *                     one put-object instead of as a multipart upload. Defaults to 5 MB server-side
 * @param contentTypes content types accepted, or empty for any. Refused before the body is read
 */
public record RouteUpload(String bucket, String keyPrefix, long maxBytes, long partSize, List<String> contentTypes) {
}
