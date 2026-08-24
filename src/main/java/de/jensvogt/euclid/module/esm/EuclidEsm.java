package de.jensvogt.euclid.module.esm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.esm.CompleteUploadRequest;
import de.jensvogt.euclid.dto.esm.CompleteUploadResponse;
import de.jensvogt.euclid.dto.esm.CreateBucketRequest;
import de.jensvogt.euclid.dto.esm.CreateBucketResponse;
import de.jensvogt.euclid.dto.esm.CreateUploadRequest;
import de.jensvogt.euclid.dto.esm.CreateUploadResponse;
import de.jensvogt.euclid.dto.esm.DeleteBucketRequest;
import de.jensvogt.euclid.dto.esm.DeleteObjectRequest;
import de.jensvogt.euclid.dto.esm.GetBucketErnRequest;
import de.jensvogt.euclid.dto.esm.GetBucketErnResponse;
import de.jensvogt.euclid.dto.esm.GetBucketSizeRequest;
import de.jensvogt.euclid.dto.esm.GetBucketSizeResponse;
import de.jensvogt.euclid.dto.esm.ListBucketsRequest;
import de.jensvogt.euclid.dto.esm.ListObjectsRequest;
import de.jensvogt.euclid.dto.esm.ListObjectsResponse;
import de.jensvogt.euclid.dto.esm.PurgeBucketRequest;
import de.jensvogt.euclid.dto.esm.PurgeBucketResponse;
import de.jensvogt.euclid.dto.esm.model.Bucket;
import de.jensvogt.euclid.dto.esm.model.EsmObject;
import de.jensvogt.euclid.exception.EuclidAuthenticationException;
import de.jensvogt.euclid.http.EuclidHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * ESM (storage) operations for an authenticated {@link de.jensvogt.euclid.module.eam.EuclidSession}.
 * Mirrors euclid-cli's {@code EsmCli}.
 */
public final class EuclidEsm {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TARGET = "esm";

    private static final int DEFAULT_UPLOAD_PART_SIZE = 5 * 1024 * 1024;
    private static final int DEFAULT_CONCURRENCY = 4;

    // uploadPart() retry tuning, mirroring EsmCli.cpp: parts are the hot path of an upload
    // (thousands of calls for a large file), so a handful of quick retries turns a transient
    // failure into a brief stall instead of aborting the whole upload.
    private static final int MAX_PART_ATTEMPTS = 4;
    private static final long PART_RETRY_BASE_DELAY_MS = 500;

    private final String baseUrl;
    private final String token;
    private final String region;
    private final String accountId;
    private final String userId;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final EuclidHttpClient httpClient;

    public EuclidEsm(String baseUrl, String token, String region, String accountId, String userId,
                      String accessKeyId, String secretAccessKey, String caCertPath) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.region = region;
        this.accountId = accountId;
        this.userId = userId;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.httpClient = new EuclidHttpClient(caCertPath);
    }

    public CreateBucketResponse createBucket(String name) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(CreateBucketRequest.builder().name(name).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "create-bucket",
                requestHeaders("create-bucket", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractCreateBucketResponse(response.body());
    }

    public void deleteBucket(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteBucketRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "delete-bucket",
                requestHeaders("delete-bucket", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
    }

    public List<Bucket> listBuckets() throws IOException, InterruptedException {
        return listBuckets("", 10, 0, "name");
    }

    public List<Bucket> listBuckets(String prefix, long pageSize, long pageIndex, String sortColumn)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListBucketsRequest.builder().prefix(prefix).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "list-buckets",
                requestHeaders("list-buckets", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractBuckets(response.body());
    }

    public GetBucketErnResponse getBucketErn(String name) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetBucketErnRequest.builder().name(name).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "get-bucket-ern",
                requestHeaders("get-bucket-ern", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractGetBucketErnResponse(response.body());
    }

    public GetBucketSizeResponse getBucketSize(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetBucketSizeRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "get-bucket-size",
                requestHeaders("get-bucket-size", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractGetBucketSizeResponse(response.body());
    }

    public ListObjectsResponse listObjects(String bucketErn) throws IOException, InterruptedException {
        return listObjects(bucketErn, "", 10, 0, "name");
    }

    public ListObjectsResponse listObjects(String bucketErn, String prefix, long pageSize, long pageIndex, String sortColumn)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListObjectsRequest.builder().bucketErn(bucketErn).prefix(prefix).pageSize(pageSize)
                        .pageIndex(pageIndex).sortColumn(sortColumn).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "list-objects",
                requestHeaders("list-objects", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractListObjectsResponse(response.body());
    }

    public void deleteObject(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteObjectRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "delete-object",
                requestHeaders("delete-object", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
    }

    public PurgeBucketResponse purgeBucket(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(PurgeBucketRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "purge-bucket",
                requestHeaders("purge-bucket", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractPurgeBucketResponse(response.body());
    }

    /**
     * Uploads a local file to a bucket, transparently splitting it into parts for a multipart
     * upload. Mirrors euclid-cli's {@code upload-file} action.
     */
    public CompleteUploadResponse uploadFile(String bucketErn, String key, Path file)
            throws IOException, InterruptedException {
        return uploadFile(bucketErn, key, file, DEFAULT_UPLOAD_PART_SIZE, DEFAULT_CONCURRENCY);
    }

    /**
     * Uploads a local file to a bucket, splitting it into parts of {@code partSize} bytes and
     * uploading up to {@code concurrency} parts at a time. Mirrors euclid-cli's {@code
     * upload-file} action, including its create-upload/upload-part/complete-upload orchestration.
     */
    public CompleteUploadResponse uploadFile(String bucketErn, String key, Path file, int partSize, int concurrency)
            throws IOException, InterruptedException {
        int boundedConcurrency = Math.max(1, concurrency);
        String uploadId = createUpload(bucketErn, key, boundedConcurrency).uploadId();

        ExecutorService executor = Executors.newFixedThreadPool(boundedConcurrency);
        Semaphore slots = new Semaphore(boundedConcurrency);
        List<Future<Boolean>> inFlight = new ArrayList<>();
        try {
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[partSize];
                long partNumber = 1;
                boolean any = false;
                int read;
                while ((read = readFully(in, buffer)) > 0) {
                    any = true;
                    byte[] data = Arrays.copyOf(buffer, read);
                    long thisPart = partNumber++;
                    slots.acquire();
                    inFlight.add(executor.submit(() -> {
                        try {
                            return uploadPart(uploadId, thisPart, data);
                        } finally {
                            slots.release();
                        }
                    }));
                }
                if (!any) {
                    inFlight.add(executor.submit(() -> uploadPart(uploadId, 1L, new byte[0])));
                }
            }

            boolean ok = true;
            for (Future<Boolean> future : inFlight) {
                if (!future.get()) {
                    ok = false;
                }
            }
            if (!ok) {
                throw new IOException("upload-file failed: one or more parts could not be uploaded");
            }
        } catch (ExecutionException e) {
            throw new IOException("upload-file failed", e.getCause());
        } finally {
            executor.shutdown();
        }

        return completeUpload(uploadId);
    }

    // Fills buffer as far as possible before EOF - a plain in.read(buffer) may return short even
    // mid-file for some stream implementations, and a short, non-final part would silently corrupt
    // the upload.
    private static int readFully(InputStream in, byte[] buffer) throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int read = in.read(buffer, total, buffer.length - total);
            if (read < 0) {
                break;
            }
            total += read;
        }
        return total;
    }

    // Starts a multipart upload (internal helper used by uploadFile; not a standalone action).
    private CreateUploadResponse createUpload(String bucketErn, String key, int concurrency)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(CreateUploadRequest.builder().bucketErn(bucketErn).key(key).build());
        // Declares the concurrency the upload is about to use so the gateway's autoscaler can ramp
        // storage instances toward it directly - see EsmCli::createUpload()'s doc comment.
        Map<String, String> headers = requestHeaders("create-upload", body);
        headers.put("x-euclid-expected-concurrency", Integer.toString(concurrency));
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "create-upload", headers);

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractCreateUploadResponse(response.body());
    }

    // Uploads one part of a multipart upload (internal helper used by uploadFile; not a
    // standalone action). Unlike every other action here, this does NOT send a JSON body -
    // uploadId/partNumber ride as headers and data goes straight over the wire as raw bytes.
    //
    // One deliberate deviation worth flagging: this always authenticates with the bearer token,
    // never SigV4, even when access keys are configured (unlike the rest of the SDK). Reason:
    // SigV4.sign() hashes the body as a UTF-8 String, which is lossy for arbitrary binary bytes;
    // and the reference EsmCli.cpp itself never SigV4-signs anything, only ever using the bearer
    // token - so bearer-only for this one action matches the CLI it's syncing against rather than
    // inventing new behavior.
    private boolean uploadPart(String uploadId, long partNumber, byte[] data) throws InterruptedException {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/octet-stream");
        headers.put("x-euclid-upload-id", uploadId);
        headers.put("x-euclid-part-number", Long.toString(partNumber));
        headers.put("Authorization", "Bearer " + token);
        if (region != null) {
            headers.put("x-euclid-region", region);
        }
        if (accountId != null) {
            headers.put("x-euclid-account-id", accountId);
        }
        if (userId != null) {
            headers.put("x-euclid-user-id", userId);
        }

        for (int attempt = 1; attempt <= MAX_PART_ATTEMPTS; attempt++) {
            boolean lastAttempt = attempt == MAX_PART_ATTEMPTS;
            try {
                HttpResponse<String> response = httpClient.postBinary(baseUrl + "/", data, "esm", "upload-part", headers);
                if (response.statusCode() / 100 == 2) {
                    return true;
                }
                // A 4xx means the request itself is wrong - retrying won't change that. 5xx is the
                // transient kind retries are for.
                boolean retryable = response.statusCode() >= 500;
                if (!retryable || lastAttempt) {
                    return false;
                }
            } catch (IOException e) {
                if (lastAttempt) {
                    return false;
                }
            }
            Thread.sleep(PART_RETRY_BASE_DELAY_MS * attempt);
        }
        return false;
    }

    // Completes a multipart upload, assembling its parts into the final object (internal helper
    // used by uploadFile; not a standalone action).
    private CompleteUploadResponse completeUpload(String uploadId) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(CompleteUploadRequest.builder().uploadId(uploadId).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "complete-upload",
                requestHeaders("complete-upload", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractCompleteUploadResponse(response.body());
    }

    private static CreateBucketResponse extractCreateBucketResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return CreateBucketResponse.builder().name(textOrNull(root, "name")).ern(textOrNull(root, "ern")).build();
    }

    private static GetBucketErnResponse extractGetBucketErnResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return GetBucketErnResponse.builder().ern(textOrNull(root, "ern")).build();
    }

    private static GetBucketSizeResponse extractGetBucketSizeResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return GetBucketSizeResponse.builder().ern(textOrNull(root, "ern")).size(root.path("size").asLong(0)).build();
    }

    private static PurgeBucketResponse extractPurgeBucketResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return PurgeBucketResponse.builder().ern(textOrNull(root, "ern")).count(root.path("count").asLong(0)).build();
    }

    private static CreateUploadResponse extractCreateUploadResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return CreateUploadResponse.builder().uploadId(textOrNull(root, "uploadId")).bucketErn(textOrNull(root, "bucketErn"))
                .key(textOrNull(root, "key")).build();
    }

    private static CompleteUploadResponse extractCompleteUploadResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return CompleteUploadResponse.builder().ern(textOrNull(root, "ern")).bucketErn(textOrNull(root, "bucketErn"))
                .key(textOrNull(root, "key")).size(root.path("size").asLong(0)).status(textOrNull(root, "status"))
                .contentType(textOrNull(root, "contentType")).md5Sum(textOrNull(root, "md5Sum")).build();
    }

    private static ListObjectsResponse extractListObjectsResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return ListObjectsResponse.builder().objects(toObjectList(root.get("objects")))
                .total(root.path("total").asLong(0)).build();
    }

    private static List<EsmObject> toObjectList(JsonNode objectsNode) {
        List<EsmObject> objects = new ArrayList<>();
        if (objectsNode != null && objectsNode.isArray()) {
            for (JsonNode objectNode : objectsNode) {
                objects.add(new EsmObject(
                        textOrNull(objectNode, "ern"),
                        textOrNull(objectNode, "bucketErn"),
                        textOrNull(objectNode, "key"),
                        objectNode.path("size").asLong(0),
                        textOrNull(objectNode, "status"),
                        textOrNull(objectNode, "contentType"),
                        textOrNull(objectNode, "md5Sum"),
                        textOrNull(objectNode, "created"),
                        textOrNull(objectNode, "modified")));
            }
        }
        return objects;
    }

    private static List<Bucket> extractBuckets(String responseBody) throws IOException {
        JsonNode bucketsNode = OBJECT_MAPPER.readTree(responseBody).get("buckets");
        List<Bucket> buckets = new ArrayList<>();
        if (bucketsNode != null && bucketsNode.isArray()) {
            for (JsonNode bucketNode : bucketsNode) {
                buckets.add(new Bucket(
                        textOrNull(bucketNode, "region"),
                        textOrNull(bucketNode, "owner"),
                        textOrNull(bucketNode, "name"),
                        textOrNull(bucketNode, "ern"),
                        bucketNode.path("size").asLong(0),
                        bucketNode.path("objects").asLong(0),
                        textOrNull(bucketNode, "created"),
                        textOrNull(bucketNode, "modified")));
            }
        }
        return buckets;
    }

    /**
     * Builds the headers for one JSON-body request/action: routing headers plus authentication.
     * <p>
     * Signs with SigV4 (accessKeyId/secretAccessKey) when both are configured, mirroring how
     * euclid-cli authenticates service calls; falls back to the bearer token otherwise. Not used
     * by uploadPart(), which sends a binary body - see its doc comment.
     */
    private Map<String, String> requestHeaders(String action, String body) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        if (region != null) {
            headers.put("x-euclid-region", region);
        }
        if (accountId != null) {
            headers.put("x-euclid-account-id", accountId);
        }
        if (userId != null) {
            headers.put("x-euclid-user-id", userId);
        }

        if (accessKeyId != null && !accessKeyId.isEmpty() && secretAccessKey != null && !secretAccessKey.isEmpty()) {
            SignableRequest signable = new SignableRequest("POST", "/");
            headers.forEach(signable::header);
            signable.header("host", hostHeader());
            signable.header("x-euclid-target", TARGET);
            signable.header("x-euclid-action", action);
            signable.body(body);
            SigV4.sign(signable, accessKeyId, secretAccessKey, region, TARGET);
            headers.put("x-amz-date", signable.header("x-amz-date"));
            headers.put("x-amz-content-sha256", signable.header("x-amz-content-sha256"));
            headers.put("Authorization", signable.header("authorization"));
        } else {
            headers.put("Authorization", "Bearer " + token);
        }
        return headers;
    }

    // The literal "Host" header java.net.http will put on the wire, derived the same way it
    // derives it (from the URI's authority) so the value we sign here matches what the server
    // actually receives.
    private String hostHeader() {
        URI uri = URI.create(baseUrl);
        int port = uri.getPort();
        return port == -1 ? uri.getHost() : uri.getHost() + ":" + port;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
