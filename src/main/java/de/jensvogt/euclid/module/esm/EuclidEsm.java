package de.jensvogt.euclid.module.esm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.com.Variant;
import de.jensvogt.euclid.dto.esm.AddBucketTagRequest;
import de.jensvogt.euclid.dto.esm.CompleteDownloadRequest;
import de.jensvogt.euclid.dto.esm.CompleteUploadRequest;
import de.jensvogt.euclid.dto.esm.CopyObjectRequest;
import de.jensvogt.euclid.dto.esm.CompleteUploadResponse;
import de.jensvogt.euclid.dto.esm.CreateBucketRequest;
import de.jensvogt.euclid.dto.esm.CreateBucketResponse;
import de.jensvogt.euclid.dto.esm.CreateDownloadRequest;
import de.jensvogt.euclid.dto.esm.CreateDownloadResponse;
import de.jensvogt.euclid.dto.esm.CreateUploadRequest;
import de.jensvogt.euclid.dto.esm.CreateUploadResponse;
import de.jensvogt.euclid.dto.esm.DeleteBucketRequest;
import de.jensvogt.euclid.dto.esm.DeleteBucketTagRequest;
import de.jensvogt.euclid.dto.esm.DeleteObjectAttributeRequest;
import de.jensvogt.euclid.dto.esm.DeleteObjectRequest;
import de.jensvogt.euclid.dto.esm.GetBucketErnRequest;
import de.jensvogt.euclid.dto.esm.GetBucketErnResponse;
import de.jensvogt.euclid.dto.esm.GetBucketSizeRequest;
import de.jensvogt.euclid.dto.esm.GetBucketSizeResponse;
import de.jensvogt.euclid.dto.esm.GetObjectCountRequest;
import de.jensvogt.euclid.dto.esm.GetObjectCountResponse;
import de.jensvogt.euclid.dto.esm.ListBucketsRequest;
import de.jensvogt.euclid.dto.esm.ListBucketsResponse;
import de.jensvogt.euclid.dto.esm.ListObjectAttributesRequest;
import de.jensvogt.euclid.dto.esm.ListObjectAttributesResponse;
import de.jensvogt.euclid.dto.esm.ListObjectsRequest;
import de.jensvogt.euclid.dto.esm.ListObjectsResponse;
import de.jensvogt.euclid.dto.esm.ListSubscriptionsRequest;
import de.jensvogt.euclid.dto.esm.ListSubscriptionsResponse;
import de.jensvogt.euclid.dto.esm.ObjectAttributeRequest;
import de.jensvogt.euclid.dto.esm.ObjectAttributeResponse;
import de.jensvogt.euclid.dto.esm.PurgeBucketRequest;
import de.jensvogt.euclid.dto.esm.RenameObjectRequest;
import de.jensvogt.euclid.dto.esm.PurgeBucketResponse;
import de.jensvogt.euclid.dto.esm.SetBucketTagRequest;
import de.jensvogt.euclid.dto.esm.SubscribeRequest;
import de.jensvogt.euclid.dto.esm.SubscribeResponse;
import de.jensvogt.euclid.dto.esm.UnsubscribeRequest;
import de.jensvogt.euclid.dto.esm.model.Bucket;
import de.jensvogt.euclid.dto.esm.model.EsmObject;
import de.jensvogt.euclid.dto.esm.model.Subscription;
import de.jensvogt.euclid.exception.EuclidServiceException;
import de.jensvogt.euclid.http.EuclidHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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

    /**
     * A singleton instance of {@code ObjectMapper} from the Jackson library used for
     * serializing Java objects to JSON and deserializing JSON to Java objects.
     * <p>
     * This instance is thread-safe and can be reused throughout the application
     * to avoid the overhead of creating multiple instances.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * A constant string representing the target identifier.
     * This value is set to "esm" and remains unchanged throughout the program's execution.
     * It is typically used to denote a specific configuration, mode, or target environment
     * within the application.
     */
    private static final String TARGET = "esm";

    /**
     * The default size, in bytes, for each part in a multipart upload.
     * This value is used to determine the chunk size when splitting large files
     * into smaller parts for upload. The default size is set to 5 MiB (5 * 1024 * 1024 bytes).
     * Larger values may improve performance for large files, but require more memory.
     */
    private static final int DEFAULT_UPLOAD_PART_SIZE = 5 * 1024 * 1024;

    /**
     * Represents the default level of concurrency for processing tasks.
     * This value is typically used to define the number of parallel threads
     * or tasks that can be executed concurrently, depending on the application's
     * design and requirements.
     * <p>
     * The default value of 4 is chosen as a reasonable balance for environments with
     * multiple processor cores, allowing efficient multitasking without overwhelming
     * system resources.
     */
    private static final int DEFAULT_CONCURRENCY = 4;

    /**
     * The maximum number of attempts allowed for a single step of the create-upload/upload-part/
     * complete-upload sequence. This value determines how many times the system will retry in the
     * case of a transient failure before aborting.
     * <p>
     * Designed to enhance the robustness of the upload process for large files, where
     * thousands of part uploads may occur. Allows a few quick retries to handle transient
     * issues, minimizing the risk of completely aborting the entire upload process. The calls
     * bracketing the parts - on the download side too - retry on the same terms: they run once per
     * transfer rather than once per part, but giving up on a transient failure there discards the
     * whole file.
     */
    private static final int MAX_PART_ATTEMPTS = 4;

    /**
     * Represents the base delay in milliseconds before retrying a failed operation
     * associated with a part or component. This value is used as the initial delay
     * in retry mechanisms and may be modified or increased based on subsequent
     * retry attempts or exponential backoff strategies.
     */
    private static final long PART_RETRY_BASE_DELAY_MS = 500;

    /**
     * The HTTP status a get-object answers with when the object is at or above the caller's
     * declared part size. Not an error in the download path: it is the server saying the object
     * needs the multipart flow rather than a single response, which is how
     * {@link #downloadFile(String, String, Path, int, int)} decides between the two.
     */
    private static final int PAYLOAD_TOO_LARGE = 413;

    /**
     * Represents the base URL used for making network requests or as a foundational
     * endpoint in the application. This URL typically serves as the starting point
     * for constructing specific API endpoints or resource paths.
     */
    private final String baseUrl;

    /**
     * A secure, immutable string that represents an access or authentication token.
     * This token is typically used for verifying identity or granting access
     * to restricted resources or services within an application.
     */
    private final String token;

    /**
     * Represents the geographical region or area associated with this instance.
     * This value is immutable and must be initialized at the time of object creation.
     */
    private final String region;

    /**
     * Represents the unique identifier for an account.
     * This identifier is immutable and is used to distinguish
     * individual accounts within the system.
     */
    private final String accountId;

    /**
     * Represents the unique identifier assigned to a user.
     * This value is immutable and serves as a key for associating
     * user-specific data within the system.
     */
    private final String userId;

    /**
     * Represents the access key identifier used for authentication purposes.
     * This key is typically provided by a cloud or API service
     * to grant authorized access to resources.
     */
    private final String accessKeyId;

    /**
     * A private and immutable string variable that stores the secret access key
     * used for authenticating and authorizing access to secure resources or services.
     * This key should be handled with care to prevent unauthorized access.
     * Ensure that it is kept confidential and not exposed in logs, error messages,
     * or other publicly accessible outputs.
     */
    private final String secretAccessKey;

    /**
     * An immutable instance of EuclidHttpClient used for executing HTTP requests.
     * This client is designed to handle communication with external APIs or services,
     * providing methods to send and receive HTTP data.
     * It is initialized once and intended to be reused wherever necessary,
     * ensuring efficient resource management and consistent configuration across requests.
     */
    private final EuclidHttpClient httpClient;

    /**
     * The session's active namespace, or {@code null}/empty if unscoped. Sent as the
     * {@code x-euclid-namespace} header on every request, exactly as {@link
     * de.jensvogt.euclid.module.eam.EuclidSession} does for its own EAM calls - without it, every
     * bucket this client creates or looks up lands in the unnamed/default namespace regardless of
     * what namespace the session was scoped to at login.
     */
    private final String nameSpace;

    /**
     * Constructs an instance of the EuclidEsm class with the specified parameters.
     *
     * @param baseUrl the base URL for the Euclid API
     * @param token the authentication token for accessing the Euclid service
     * @param region the region identifier for the Euclid service
     * @param accountId the account ID associated with the Euclid service
     * @param userId the user ID associated with the Euclid account
     * @param accessKeyId the access key ID for authentication
     * @param secretAccessKey the secret access key for authentication
     * @param caCertPath the file path to the CA certificate used for secure connections
     * @param nameSpace the session's active namespace, or {@code null}/empty if unscoped
     */
    public EuclidEsm(String baseUrl, String token, String region, String accountId, String userId,
                      String accessKeyId, String secretAccessKey, String caCertPath, String nameSpace) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.region = region;
        this.accountId = accountId;
        this.userId = userId;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.nameSpace = nameSpace;
        this.httpClient = new EuclidHttpClient(caCertPath);
    }

    /**
     * Creates a new bucket with the specified name.
     *
     * @param name the name of the bucket to be created
     * @return a {@link CreateBucketResponse} object containing the details of the newly created bucket
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted
     */
    public CreateBucketResponse createBucket(String name) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(CreateBucketRequest.builder().name(name).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "create-bucket",
                requestHeaders("create-bucket", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "create-bucket", response.statusCode(), response.body());
        }

        return extractCreateBucketResponse(response.body());
    }

    /**
     * Deletes a bucket identified by the given ERN (Extended Resource Name).
     *
     * @param ern The Extended Resource Name (ERN) of the bucket to be deleted.
     * @throws IOException If an I/O error occurs while making the HTTP request.
     * @throws InterruptedException If the operation is interrupted during the process.
     */
    public void deleteBucket(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteBucketRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "delete-bucket",
                requestHeaders("delete-bucket", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "delete-bucket", response.statusCode(), response.body());
        }
    }

    /**
     * Retrieves a list of storage buckets.
     * <p>
     * This method fetches a list of buckets with default parameters: an
     * empty filter, a maximum of 10 results, an offset of 0, and ordering
     * by name.
     *
     * @return a {@code ListBucketsResponse} carrying the buckets and how many exist in total.
     * @throws IOException if an I/O error occurs during the operation.
     * @throws InterruptedException if the operation is interrupted.
     */
    public ListBucketsResponse listBuckets() throws IOException, InterruptedException {
        return listBuckets("", 10, 0, "name");
    }

    /**
     * Retrieves a list of buckets based on the provided parameters.
     *
     * @param prefix     A string used to filter buckets by their name. Only buckets with names
     *                   starting with this prefix will be included.
     * @param pageSize   The number of buckets to return per page.
     * @param pageIndex  The zero-based index of the page to retrieve.
     * @param sortColumn The column by which to sort the results.
     * @return A {@code ListBucketsResponse} carrying the matching buckets and their total.
     * @throws IOException            If an input or output exception occurs during the operation.
     * @throws InterruptedException   If the calling thread is interrupted.
     */
    public ListBucketsResponse listBuckets(String prefix, long pageSize, long pageIndex, String sortColumn)
            throws IOException, InterruptedException {
        return listBuckets(prefix, pageSize, pageIndex, sortColumn, "asc");
    }

    /**
     * Retrieves a list of buckets based on the provided parameters, in a chosen sort direction.
     *
     * @param prefix        A string used to filter buckets by their name. Only buckets with names
     *                      starting with this prefix will be included.
     * @param pageSize      The number of buckets to return per page.
     * @param pageIndex     The zero-based index of the page to retrieve.
     * @param sortColumn    The column by which to sort the results.
     * @param sortDirection The direction to sort in, {@code "asc"} or {@code "desc"}.
     * @return A {@code ListBucketsResponse} carrying the matching buckets and their total.
     * @throws IOException            If an input or output exception occurs during the operation.
     * @throws InterruptedException   If the calling thread is interrupted.
     */
    public ListBucketsResponse listBuckets(String prefix, long pageSize, long pageIndex, String sortColumn,
                                           String sortDirection) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListBucketsRequest.builder().prefix(prefix).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).sortDirection(sortDirection).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "list-buckets",
                requestHeaders("list-buckets", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "list-buckets", response.statusCode(), response.body());
        }

        return extractListBucketsResponse(response.body());
    }

    /**
     * Retrieves the bucket ERN (Entity Resource Name) for the given bucket name.
     *
     * @param name The name of the bucket for which the ERN is being requested.
     * @return An instance of {@code GetBucketErnResponse} containing the ERN of the specified bucket.
     * @throws IOException If an I/O error occurs during the HTTP request.
     * @throws InterruptedException If the operation is interrupted while waiting for a response.
     */
    public GetBucketErnResponse getBucketErn(String name) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetBucketErnRequest.builder().name(name).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "get-bucket-ern",
                requestHeaders("get-bucket-ern", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "get-bucket-ern", response.statusCode(), response.body());
        }

        return extractGetBucketErnResponse(response.body());
    }

    /**
     * Retrieves the size of the bucket associated with the specified ERN (External Resource Name).
     *
     * @param ern The ERN (External Resource Name) identifying the bucket for which the size is to be retrieved.
     * @return A {@code GetBucketSizeResponse} object containing the size and related details of the bucket.
     * @throws IOException If an input or output exception occurs during the HTTP request.
     * @throws InterruptedException If the thread executing the request is interrupted.
     */
    public GetBucketSizeResponse getBucketSize(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetBucketSizeRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "get-bucket-size",
                requestHeaders("get-bucket-size", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "get-bucket-size", response.statusCode(), response.body());
        }

        return extractGetBucketSizeResponse(response.body());
    }

    /**
     * Lists the objects within the specified bucket.
     *
     * @param bucketErn the ARN of the bucket for which to fetch the object list
     * @return a {@code ListObjectsResponse} containing the list of objects in the specified bucket
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListObjectsResponse listObjects(String bucketErn) throws IOException, InterruptedException {
        return listObjects(bucketErn, "", 10, 0, "name");
    }

    /**
     * Lists the objects in a specified bucket with optional filtering and pagination.
     *
     * @param bucketErn    The ARN (Amazon Resource Name) of the bucket from which to list objects.
     * @param prefix       The prefix to filter the objects by (e.g., to list objects with names starting with this prefix).
     * @param pageSize     The maximum number of objects to include in each paginated response.
     * @param pageIndex    The zero-based index of the page to retrieve.
     * @param sortColumn   The column by which to sort the objects in the listing.
     * @return A {@code ListObjectsResponse} object containing the details of the listed objects, including metadata and pagination information.
     * @throws IOException If there is a communication issue during the HTTP request.
     * @throws InterruptedException If the current thread is interrupted while waiting for a response.
     */
    public ListObjectsResponse listObjects(String bucketErn, String prefix, long pageSize, long pageIndex, String sortColumn)
            throws IOException, InterruptedException {
        return listObjects(bucketErn, prefix, pageSize, pageIndex, sortColumn, "asc", false);
    }

    /**
     * Lists the objects in a specified bucket with optional filtering and pagination, in a chosen
     * sort direction and optionally including directory keys.
     *
     * @param bucketErn          The ARN (Amazon Resource Name) of the bucket from which to list objects.
     * @param prefix             The prefix to filter the objects by (e.g., to list objects with names starting with this prefix).
     * @param pageSize           The maximum number of objects to include in each paginated response.
     * @param pageIndex          The zero-based index of the page to retrieve.
     * @param sortColumn         The column by which to sort the objects in the listing.
     * @param sortDirection      The direction to sort in, {@code "asc"} or {@code "desc"}.
     * @param includeDirectories Whether directory keys are listed alongside real objects. Object keys
     *                           are opaque strings, so a bucket only has "directories" in the sense
     *                           that keys share a prefix - left out by default.
     * @return A {@code ListObjectsResponse} object containing the details of the listed objects, including metadata and pagination information.
     * @throws IOException If there is a communication issue during the HTTP request.
     * @throws InterruptedException If the current thread is interrupted while waiting for a response.
     */
    public ListObjectsResponse listObjects(String bucketErn, String prefix, long pageSize, long pageIndex,
                                           String sortColumn, String sortDirection, boolean includeDirectories)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListObjectsRequest.builder().bucketErn(bucketErn).prefix(prefix).pageSize(pageSize)
                        .pageIndex(pageIndex).sortColumn(sortColumn).sortDirection(sortDirection)
                        .includeDirectories(includeDirectories).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "list-objects",
                requestHeaders("list-objects", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "list-objects", response.statusCode(), response.body());
        }

        return extractListObjectsResponse(response.body());
    }

    /**
     * Deletes an object identified by the given ERN (Entity Reference Number).
     *
     * @param ern The ERN of the object to be deleted.
     * @throws IOException If an I/O error occurs during the operation.
     * @throws InterruptedException If the operation is interrupted.
     */
    public void deleteObject(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteObjectRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "delete-object",
                requestHeaders("delete-object", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "delete-object", response.statusCode(), response.body());
        }
    }

    /**
     * Sends a request to purge the contents of a bucket identified by its ERN (Euclid Resource Name).
     * This operation removes all objects within the specified bucket.
     *
     * @param ern the Euclid Resource Name (ERN) of the bucket to be purged
     * @return a {@code PurgeBucketResponse} containing details of the purged bucket, including the ERN and the count of removed objects
     * @throws IOException if a network or serialization error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public PurgeBucketResponse purgeBucket(String ern) throws IOException, InterruptedException {
        return purgeBucket(ern, "");
    }

    /**
     * Purges the objects of a bucket whose key starts with the given prefix, leaving the bucket
     * itself in place.
     *
     * @param ern the Euclid Resource Name (ERN) of the bucket to be purged
     * @param prefix only objects whose key starts with this prefix are deleted; empty purges them all
     * @return a {@code PurgeBucketResponse} containing the bucket's ERN and the number of objects left
     * @throws IOException if a network or serialization error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public PurgeBucketResponse purgeBucket(String ern, String prefix) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(PurgeBucketRequest.builder().ern(ern).prefix(prefix).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "purge-bucket",
                requestHeaders("purge-bucket", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "purge-bucket", response.statusCode(), response.body());
        }

        return extractPurgeBucketResponse(response.body());
    }

    /**
     * Copies an object, leaving the source in place. The copy gets its own bytes on disk and its
     * own ERN, so the two are independent from here on.
     * <p>
     * Both ends are permission-checked: reading out of one bucket and writing into another are
     * separate rights and this does both. An existing object at the target is refused with HTTP 409
     * rather than silently replaced - overwriting means deleting the target first and saying so.
     *
     * @param sourceBucketErn the ERN of the bucket the object is read from
     * @param sourceKey the key of the object to copy
     * @param targetBucketErn the ERN of the bucket the object is written to
     * @param targetKey the key the copy is written under
     * @return the newly stored object
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public EsmObject copyObject(String sourceBucketErn, String sourceKey, String targetBucketErn, String targetKey)
            throws IOException, InterruptedException {
        return transferObject("copy-object", sourceBucketErn, sourceKey, targetBucketErn, targetKey);
    }

    /**
     * Moves an object to another bucket or key, removing the source. The bytes on disk are not
     * copied - the same file simply answers to a different key from now on - so this is cheap
     * regardless of object size.
     * <p>
     * Refuses an existing object at the target with HTTP 409, exactly as {@link #copyObject} does.
     *
     * @param sourceBucketErn the ERN of the bucket the object is moved out of
     * @param sourceKey the key of the object to move
     * @param targetBucketErn the ERN of the bucket the object is moved into
     * @param targetKey the key the object is written under
     * @return the object at its new location
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public EsmObject moveObject(String sourceBucketErn, String sourceKey, String targetBucketErn, String targetKey)
            throws IOException, InterruptedException {
        return transferObject("move-object", sourceBucketErn, sourceKey, targetBucketErn, targetKey);
    }

    /**
     * Renames an object within its bucket - a {@link #moveObject} that cannot leave the bucket,
     * which is the whole difference between the two.
     *
     * @param bucketErn the ERN of the bucket holding the object
     * @param key the object's current key
     * @param newKey the key to rename it to
     * @return the object under its new key
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public EsmObject renameObject(String bucketErn, String key, String newKey)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                RenameObjectRequest.builder().bucketErn(bucketErn).key(key).newKey(newKey).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "rename-object",
                requestHeaders("rename-object", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "rename-object", response.statusCode(), response.body());
        }

        return toEsmObject(OBJECT_MAPPER.readTree(response.body()));
    }

    /**
     * Sends a copy-object or move-object action, which take the same request and differ only in
     * whether the source survives.
     *
     * @param action the action to send, {@code "copy-object"} or {@code "move-object"}
     * @param sourceBucketErn the ERN of the bucket the object is read from
     * @param sourceKey the key of the object to transfer
     * @param targetBucketErn the ERN of the bucket the object is written to
     * @param targetKey the key the object is written under
     * @return the stored object at its target location
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    private EsmObject transferObject(String action, String sourceBucketErn, String sourceKey, String targetBucketErn,
                                     String targetKey) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(CopyObjectRequest.builder()
                .sourceBucketErn(sourceBucketErn).sourceKey(sourceKey)
                .targetBucketErn(targetBucketErn).targetKey(targetKey).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", action,
                requestHeaders(action, body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", action, response.statusCode(), response.body());
        }

        return toEsmObject(OBJECT_MAPPER.readTree(response.body()));
    }

    /**
     * Counts the objects in a bucket. Cheaper than listing them when only the number is wanted -
     * the server counts server-side rather than paging every object back to the caller.
     *
     * @param bucketErn the Euclid Resource Name (ERN) of the bucket whose objects are counted
     * @return a {@code GetObjectCountResponse} carrying the bucket ERN and the number of objects
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public GetObjectCountResponse getObjectCount(String bucketErn) throws IOException, InterruptedException {
        return getObjectCount(bucketErn, "");
    }

    /**
     * Counts the objects in a bucket whose key starts with the given prefix.
     *
     * @param bucketErn the Euclid Resource Name (ERN) of the bucket whose objects are counted
     * @param prefix only objects whose key starts with this prefix are counted; empty counts them all
     * @return a {@code GetObjectCountResponse} carrying the bucket ERN and the number of objects
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public GetObjectCountResponse getObjectCount(String bucketErn, String prefix) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                GetObjectCountRequest.builder().ern(bucketErn).prefix(prefix).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "get-object-count",
                requestHeaders("get-object-count", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "get-object-count", response.statusCode(), response.body());
        }

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        return GetObjectCountResponse.builder().ern(textOrNull(root, "ern")).count(root.path("count").asLong(0)).build();
    }

    /**
     * Adds a tag to a bucket. A key that is already tagged keeps the value it has - use
     * {@link #setBucketTag} to overwrite.
     *
     * @param bucketErn the Euclid Resource Name (ERN) of the bucket to tag
     * @param key the tag key
     * @param value the tag value
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void addBucketTag(String bucketErn, String key, String value) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                AddBucketTagRequest.builder().ern(bucketErn).key(key).value(value).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "add-bucket-tag",
                requestHeaders("add-bucket-tag", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "add-bucket-tag", response.statusCode(), response.body());
        }
    }

    /**
     * Sets a tag on a bucket, overwriting any value the key already had.
     *
     * @param bucketErn the Euclid Resource Name (ERN) of the bucket to tag
     * @param key the tag key
     * @param value the tag value
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void setBucketTag(String bucketErn, String key, String value) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                SetBucketTagRequest.builder().ern(bucketErn).key(key).value(value).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "set-bucket-tag",
                requestHeaders("set-bucket-tag", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "set-bucket-tag", response.statusCode(), response.body());
        }
    }

    /**
     * Deletes a tag from a bucket.
     *
     * @param bucketErn the Euclid Resource Name (ERN) of the bucket the tag belongs to
     * @param key the tag key to delete
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void deleteBucketTag(String bucketErn, String key) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                DeleteBucketTagRequest.builder().ern(bucketErn).key(key).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "delete-bucket-tag",
                requestHeaders("delete-bucket-tag", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "delete-bucket-tag", response.statusCode(), response.body());
        }
    }

    /**
     * Adds a user-defined attribute to an object. An attribute of that name already on the object
     * keeps the value it has - use {@link #setObjectAttribute} to overwrite.
     *
     * @param ern the Euclid Resource Name (ERN) of the object carrying the attribute
     * @param name the attribute name
     * @param value the typed attribute value
     * @return an {@code ObjectAttributeResponse} carrying the attribute as the server stored it
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public ObjectAttributeResponse addObjectAttribute(String ern, String name, Variant value)
            throws IOException, InterruptedException {
        return objectAttribute("add-object-attribute", ern, name, value);
    }

    /**
     * Sets a user-defined attribute on an object, overwriting any value an attribute of that name
     * already had.
     *
     * @param ern the Euclid Resource Name (ERN) of the object carrying the attribute
     * @param name the attribute name
     * @param value the typed attribute value
     * @return an {@code ObjectAttributeResponse} carrying the attribute as the server stored it
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public ObjectAttributeResponse setObjectAttribute(String ern, String name, Variant value)
            throws IOException, InterruptedException {
        return objectAttribute("set-object-attribute", ern, name, value);
    }

    /**
     * Lists every user-defined attribute of an object.
     *
     * @param ern the Euclid Resource Name (ERN) of the object whose attributes are listed
     * @return a {@code ListObjectAttributesResponse} carrying the attributes keyed by name
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public ListObjectAttributesResponse listObjectAttributes(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(ListObjectAttributesRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "list-object-attributes",
                requestHeaders("list-object-attributes", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "list-object-attributes", response.statusCode(), response.body());
        }

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        return ListObjectAttributesResponse.builder().ern(textOrNull(root, "ern"))
                .attributes(toVariantMap(root.get("attributes"))).total(root.path("total").asLong(0)).build();
    }

    /**
     * Deletes a user-defined attribute from an object.
     *
     * @param ern the Euclid Resource Name (ERN) of the object carrying the attribute
     * @param name the name of the attribute to delete
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void deleteObjectAttribute(String ern, String name) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                DeleteObjectAttributeRequest.builder().ern(ern).name(name).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "delete-object-attribute",
                requestHeaders("delete-object-attribute", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "delete-object-attribute", response.statusCode(), response.body());
        }
    }

    /**
     * Subscribes a queue or a topic to a bucket's object events, so an object created in that
     * bucket is announced to the target from then on.
     *
     * @param bucketErn the Euclid Resource Name (ERN) of the bucket whose events are subscribed to
     * @param type the target resource type, {@code "queue"} or {@code "topic"}
     * @param targetErn the ERN of the queue or topic the events are delivered to
     * @return a {@code SubscribeResponse} carrying the new subscription's own ERN
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public SubscribeResponse subscribe(String bucketErn, String type, String targetErn)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                SubscribeRequest.builder().sourceErn(bucketErn).type(type).targetErn(targetErn).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "subscribe",
                requestHeaders("subscribe", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "subscribe", response.statusCode(), response.body());
        }

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        return SubscribeResponse.builder().ern(textOrNull(root, "ern")).sourceErn(textOrNull(root, "sourceErn"))
                .type(textOrNull(root, "type")).targetErn(textOrNull(root, "targetErn")).build();
    }

    /**
     * Removes a subscription, identified by the ERN {@link #subscribe} returned - not the ERN of
     * the bucket or of the target it connects.
     *
     * @param ern the Euclid Resource Name (ERN) of the subscription to remove
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void unsubscribe(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(UnsubscribeRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "unsubscribe",
                requestHeaders("unsubscribe", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "unsubscribe", response.statusCode(), response.body());
        }
    }

    /**
     * Lists every subscription currently registered on a bucket.
     *
     * @param bucketErn the Euclid Resource Name (ERN) of the bucket whose subscriptions are listed
     * @return a {@code ListSubscriptionsResponse} carrying the subscriptions and their total
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public ListSubscriptionsResponse listSubscriptions(String bucketErn) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(ListSubscriptionsRequest.builder().bucketErn(bucketErn).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "list-subscriptions",
                requestHeaders("list-subscriptions", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "list-subscriptions", response.statusCode(), response.body());
        }

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        return ListSubscriptionsResponse.builder().subscriptions(toSubscriptionList(root.get("subscriptions")))
                .total(root.path("total").asLong(0)).build();
    }

    /**
     * Uploads an object's bytes in a single request, skipping the create-upload/upload-part/
     * complete-upload sequence entirely. The counterpart of {@link #getObject} on the write side,
     * and what {@link #uploadFile} would be for a file small enough not to need splitting.
     * <p>
     * Like upload-part, the bytes go over the wire as a raw {@code application/octet-stream} body
     * with the bucket and key riding as headers, rather than base64 in a JSON field.
     *
     * @param bucketErn the Euclid Resource Name (ERN) of the target bucket
     * @param key the key (path) within the bucket to store the object under
     * @param data the object's bytes
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void putObject(String bucketErn, String key, byte[] data) throws IOException, InterruptedException {
        Map<String, String> headers = binaryRequestHeaders();
        headers.put("x-euclid-bucket-ern", bucketErn);
        headers.put("x-euclid-key", key);
        HttpResponse<String> response = httpClient.postBinary(baseUrl + "/", data, "esm", "put-object", headers);

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "put-object", response.statusCode(), response.body());
        }
    }

    /**
     * Downloads an object's bytes in a single request, skipping the create-download/download-part/
     * complete-download sequence entirely.
     * <p>
     * The size limit is enforced by the server, not here: a download's size isn't known until asked
     * (unlike an upload, where the caller has already stat'd the local file), so the caller declares
     * how large a response it is willing to take and an object at or above that size comes back as
     * HTTP 413 rather than being streamed. {@link #downloadFile} uses exactly that to decide whether
     * an object needs the multipart path.
     *
     * @param bucketErn the Euclid Resource Name (ERN) of the bucket holding the object
     * @param key the key of the object to download
     * @param maxInlineSize the largest object, in bytes, to accept in one response
     * @return the object's bytes
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public byte[] getObject(String bucketErn, String key, long maxInlineSize) throws IOException, InterruptedException {
        HttpResponse<byte[]> response = getObjectResponse(bucketErn, key, maxInlineSize);

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "get-object", response.statusCode(),
                    new String(response.body(), StandardCharsets.UTF_8));
        }

        return response.body();
    }

    /**
     * Uploads a local file to a specified bucket using its Euclid Resource Name (ERN) and key.
     * This method handles splitting the file into parts and performing the multipart upload process.
     *
     * @param bucketErn the Euclid Resource Name (ERN) of the target bucket
     * @param key the key (path) within the bucket where the file will be stored
     * @param file the path to the local file to be uploaded
     * @return a {@code CompleteUploadResponse} containing details about the completed upload,
     *         such as the upload ID and the location of the uploaded object
     * @throws IOException if an I/O error occurs during the file upload process
     * @throws InterruptedException if the operation is interrupted during execution
     */
    public CompleteUploadResponse uploadFile(String bucketErn, String key, Path file)
            throws IOException, InterruptedException {
        return uploadFile(bucketErn, key, file, DEFAULT_UPLOAD_PART_SIZE, DEFAULT_CONCURRENCY);
    }

    /**
     * Uploads a file to a specified bucket in a multipart upload process. This method splits the file
     * into parts of a specified size and uploads them concurrently.
     *
     * @param bucketErn   The unique identifier of the bucket where the file will be uploaded.
     * @param key         The key (path/identifier) for the file within the bucket.
     * @param file        The path to the file to be uploaded.
     * @param partSize    The size (in bytes) of each part to be uploaded.
     * @param concurrency The maximum number of parts that can be uploaded concurrently. 
     *                    This value will be bounded to a minimum of 1.
     * @return A {@code CompleteUploadResponse} object containing information about the completed upload.
     * @throws IOException           If an I/O error occurs during the upload process.
     * @throws InterruptedException  If the thread executing this method is interrupted while waiting.
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

    /**
     * Downloads an object from a bucket to a local file, using default part size and concurrency.
     *
     * @param bucketErn the Euclid Resource Name (ERN) of the bucket holding the object
     * @param key the key of the object to download
     * @param file the local path to write the object to; parent directories are created as needed
     * @return the number of bytes written to {@code file}
     * @throws IOException if an I/O error occurs during the download
     * @throws InterruptedException if the operation is interrupted during execution
     */
    public long downloadFile(String bucketErn, String key, Path file) throws IOException, InterruptedException {
        return downloadFile(bucketErn, key, file, DEFAULT_UPLOAD_PART_SIZE, DEFAULT_CONCURRENCY);
    }

    /**
     * Downloads an object from a bucket to a local file, fetching its parts concurrently.
     * <p>
     * An object that fits in a single part skips multipart entirely: get-object fetches it in one
     * round trip rather than paying for create-download plus one download-part plus
     * complete-download. Unlike {@link #uploadFile} - which stats the local file up front and so
     * knows before making any request whether it is small - a download's size isn't known until
     * asked, so the single-shot path is tried first and HTTP 413 is what says the object turned out
     * to be too large for it.
     *
     * @param bucketErn the Euclid Resource Name (ERN) of the bucket holding the object
     * @param key the key of the object to download
     * @param file the local path to write the object to; parent directories are created as needed
     * @param partSize the size (in bytes) of each part to request
     * @param concurrency the maximum number of parts to download concurrently, bounded to a minimum of 1
     * @return the number of bytes written to {@code file}
     * @throws IOException if an I/O error occurs during the download
     * @throws InterruptedException if the operation is interrupted during execution
     */
    public long downloadFile(String bucketErn, String key, Path file, int partSize, int concurrency)
            throws IOException, InterruptedException {
        int boundedConcurrency = Math.max(1, concurrency);
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }

        HttpResponse<byte[]> inline = getObjectResponse(bucketErn, key, partSize);
        if (inline.statusCode() != PAYLOAD_TOO_LARGE) {
            if (inline.statusCode() / 100 != 2) {
                throw new EuclidServiceException("esm", "get-object", inline.statusCode(),
                        new String(inline.body(), StandardCharsets.UTF_8));
            }
            Files.write(file, inline.body());
            return inline.body().length;
        }

        CreateDownloadResponse created = createDownload(bucketErn, key, boundedConcurrency);
        long totalSize = created.size();

        // Pre-sizes the destination so the workers below can each write their own byte range at the
        // right offset whatever order they finish in - the download's answer to uploadFile() reading
        // its source sequentially while letting the parts themselves complete out of order.
        try (RandomAccessFile preallocate = new RandomAccessFile(file.toFile(), "rw")) {
            preallocate.setLength(totalSize);
        }

        ExecutorService executor = Executors.newFixedThreadPool(boundedConcurrency);
        Semaphore slots = new Semaphore(boundedConcurrency);
        List<Future<Boolean>> inFlight = new ArrayList<>();
        try {
            long partNumber = 1;
            for (long offset = 0; offset < totalSize; offset += partSize, partNumber++) {
                long thisPart = partNumber;
                slots.acquire();
                inFlight.add(executor.submit(() -> {
                    try {
                        return downloadPart(created.downloadId(), thisPart, partSize, file);
                    } finally {
                        slots.release();
                    }
                }));
            }

            boolean ok = true;
            for (Future<Boolean> future : inFlight) {
                if (!future.get()) {
                    ok = false;
                }
            }
            if (!ok) {
                throw new IOException("download-file failed: one or more parts could not be downloaded");
            }
        } catch (ExecutionException e) {
            throw new IOException("download-file failed", e.getCause());
        } finally {
            executor.shutdown();
        }

        completeDownload(created.downloadId());
        return totalSize;
    }

    /**
     * Reads data from the given input stream into the provided buffer until the buffer is full
     * or the end of the stream is reached. This method ensures that as much of the buffer as 
     * possible is filled, as some stream implementations may return fewer bytes than requested 
     * in a single read operation.
     *
     * @param in the input stream to read data from
     * @param buffer the byte array to fill with the read data
     * @return the total number of bytes read into the buffer, or -1 if the end of the stream 
     *         is reached before any bytes are read
     * @throws IOException if an I/O error occurs while reading from the input stream
     */
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

    /**
     * Starts a multipart upload to initialize an upload process.
     *
     * @param bucketErn The ARN (Amazon Resource Name) of the bucket where the upload will be stored.
     * @param key The unique identifier for the object to be uploaded.
     * @param concurrency The number of concurrent upload parts expected for the upload process.
     * @return A {@code CreateUploadResponse} object containing details of the initiated upload.
     * @throws IOException If an I/O error occurs during the request.
     * @throws InterruptedException If the operation is interrupted while waiting for the response.
     */
    private CreateUploadResponse createUpload(String bucketErn, String key, int concurrency)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(CreateUploadRequest.builder().bucketErn(bucketErn).key(key).build());
        // Declares the concurrency the upload is about to use so the gateway's autoscaler can ramp
        // storage instances toward it directly - see EsmCli::createUpload()'s doc comment.
        Map<String, String> headers = requestHeaders("create-upload", body);
        headers.put("x-euclid-expected-concurrency", Integer.toString(concurrency));
        // Retried on 5xx: safe to repeat, since the object row the server seeds is upserted on
        // bucketErn+key, so a second attempt updates the same row rather than adding another one -
        // the only cost is the scratch directory the abandoned upload ID left behind.
        HttpResponse<String> response = postWithRetry("create-upload", body, headers);

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "create-upload", response.statusCode(), response.body());
        }

        return extractCreateUploadResponse(response.body());
    }

    /**
     * Uploads a single part of a multipart upload. This method is used internally by 
     * the uploadFile functionality and is not intended to be used as a standalone action.
     * Data is sent as raw bytes directly over the wire without including a JSON body.
     * The authentication for this method uses the bearer token instead of SigV4 signing.
     * <p>
     * Uploads one part of a multipart upload (internal helper used by uploadFile; not a
     * standalone action). Unlike every other action here, this does NOT send a JSON body -
     * uploadId/partNumber ride as headers and data goes straight over the wire as raw bytes.
     * <p>
     * One deliberate deviation worth flagging: this always authenticates with the bearer token,
     * never SigV4, even when access keys are configured (unlike the rest of the SDK). Reason:
     * SigV4.sign() hashes the body as a UTF-8 String, which is lossy for arbitrary binary bytes;
     * and the reference EsmCli.cpp itself never SigV4-signs anything, only ever using the bearer
     * token - so bearer-only for this one action matches the CLI it's syncing against rather than
     * inventing new behavior.
     * 
     * @param uploadId The unique identifier for the multipart upload session.
     * @param partNumber The sequence number of the part being uploaded within the multipart upload.
     * @param data The raw binary data of the part being uploaded.
     * @return Returns {@code true} if the part upload is successful, otherwise {@code false}.
     * @throws InterruptedException If the thread is interrupted while waiting between retries.
     */
    private boolean uploadPart(String uploadId, long partNumber, byte[] data) throws InterruptedException {
        Map<String, String> headers = binaryRequestHeaders();
        headers.put("Content-Type", "application/octet-stream");
        headers.put("x-euclid-upload-id", uploadId);
        headers.put("x-euclid-part-number", Long.toString(partNumber));

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

    /**
     * Completes a multipart upload by assembling its parts into the final object.
     * This is an internal helper method used by uploadFile and is not meant to be 
     * invoked as a standalone action.
     *
     * @param uploadId The unique identifier for the multipart upload to be completed.
     * @return A CompleteUploadResponse object containing details of the completed upload.
     * @throws IOException If an input or output exception occurs during the HTTP request.
     * @throws InterruptedException If the HTTP request is interrupted.
     */
    private CompleteUploadResponse completeUpload(String uploadId) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(CompleteUploadRequest.builder().uploadId(uploadId).build());
        // Retried on 5xx like create-upload, and for the same reason: failing here discards every
        // part already uploaded. Safe to repeat as long as the request is rejected before the server
        // takes ownership of the staged parts - it validates and hands assembly to a background
        // pass, so a 5xx from that validation means nothing was consumed. An upload the server did
        // accept fails a retry with 404 (upload not found), which is not retried.
        HttpResponse<String> response = postWithRetry("complete-upload", body, requestHeaders("complete-upload", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "complete-upload", response.statusCode(), response.body());
        }

        return extractCompleteUploadResponse(response.body());
    }

    /**
     * Posts one of the JSON actions bracketing a multipart transfer - create/complete-upload and
     * create/complete-download - retrying a 5xx or a failed request up to {@link #MAX_PART_ATTEMPTS}
     * times with a growing delay between attempts, the same treatment {@link #uploadPart} and
     * {@link #downloadPart} give the parts in between. A 4xx means the request itself is wrong, so
     * it is returned to the caller on the first attempt rather than retried.
     *
     * @param action  the ESM action to post, e.g. {@code "create-upload"}
     * @param body    the JSON request body
     * @param headers the request headers, already signed for {@code action}
     * @return the last HTTP response received, which the caller checks for success as usual
     * @throws IOException          if every attempt failed to reach the server, rethrowing the last failure
     * @throws InterruptedException if the thread is interrupted while waiting between attempts
     */
    private HttpResponse<String> postWithRetry(String action, String body, Map<String, String> headers)
            throws IOException, InterruptedException {
        for (int attempt = 1; ; attempt++) {
            boolean lastAttempt = attempt == MAX_PART_ATTEMPTS;
            try {
                HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", action, headers);
                if (response.statusCode() < 500 || lastAttempt) {
                    return response;
                }
            } catch (IOException e) {
                if (lastAttempt) {
                    throw e;
                }
            }
            Thread.sleep(PART_RETRY_BASE_DELAY_MS * attempt);
        }
    }

    /**
     * Starts a multipart download, which stages the object server-side and reports how large it is
     * so the caller knows how many parts to ask for.
     *
     * @param bucketErn the ERN of the bucket holding the object
     * @param key the key of the object to download
     * @param concurrency the number of concurrent part downloads the caller is about to use
     * @return a {@code CreateDownloadResponse} describing the download session and the object
     * @throws IOException if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    private CreateDownloadResponse createDownload(String bucketErn, String key, int concurrency)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                CreateDownloadRequest.builder().bucketErn(bucketErn).key(key).build());
        // Declares the concurrency the download is about to use so the gateway's autoscaler can ramp
        // storage instances toward it directly - see EsmCli::createDownload()'s doc comment.
        Map<String, String> headers = requestHeaders("create-download", body);
        headers.put("x-euclid-expected-concurrency", Integer.toString(concurrency));
        // Retried on 5xx like create-upload: safe to repeat, since the download session it opens is
        // server-side scratch state keyed by a fresh download ID, so a retried attempt starts a new
        // one and the abandoned session is simply never used.
        HttpResponse<String> response = postWithRetry("create-download", body, headers);

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "create-download", response.statusCode(), response.body());
        }

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        return CreateDownloadResponse.builder().downloadId(textOrNull(root, "downloadId"))
                .bucketErn(textOrNull(root, "bucketErn")).key(textOrNull(root, "key")).ern(textOrNull(root, "ern"))
                .size(root.path("size").asLong(0)).contentType(textOrNull(root, "contentType")).build();
    }

    /**
     * Downloads a single part of a multipart download and writes it at its own offset in the
     * destination file. The mirror image of {@link #uploadPart}, down to the retry policy: 5xx and
     * failed requests are retried, a 4xx is not.
     * <p>
     * Each call opens its own file handle rather than sharing one: parts land here concurrently and
     * each only ever writes its own non-overlapping byte range, so independent handles avoid
     * contending on a single shared file position for no benefit.
     *
     * @param downloadId the ID of the download session the part belongs to
     * @param partNumber the one-based sequence number of the part being fetched
     * @param partSize the part size the download was started with, which fixes each part's offset
     * @param file the destination file, already sized to hold the whole object
     * @return {@code true} if the part was fetched and written, otherwise {@code false}
     * @throws InterruptedException if the thread is interrupted while waiting between retries
     */
    private boolean downloadPart(String downloadId, long partNumber, int partSize, Path file)
            throws InterruptedException {
        Map<String, String> headers = binaryRequestHeaders();
        headers.put("x-euclid-download-id", downloadId);
        headers.put("x-euclid-part-number", Long.toString(partNumber));
        headers.put("x-euclid-part-size", Integer.toString(partSize));

        for (int attempt = 1; attempt <= MAX_PART_ATTEMPTS; attempt++) {
            boolean lastAttempt = attempt == MAX_PART_ATTEMPTS;
            try {
                HttpResponse<byte[]> response = httpClient.postForBinary(baseUrl + "/", "esm", "download-part", headers);
                if (response.statusCode() / 100 == 2) {
                    try (RandomAccessFile out = new RandomAccessFile(file.toFile(), "rw")) {
                        out.seek((partNumber - 1) * partSize);
                        out.write(response.body());
                    }
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

    /**
     * Finishes a multipart download, releasing the session's server-side scratch state.
     *
     * @param downloadId the ID of the download to complete
     * @throws IOException if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    private void completeDownload(String downloadId) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                CompleteDownloadRequest.builder().downloadId(downloadId).build());
        // Retried on 5xx like complete-upload, and for the same reason: failing here throws away
        // every part already downloaded. Safe to repeat - it only releases the session's server-side
        // scratch state, and a session already released fails a retry with 404, which is not retried.
        HttpResponse<String> response = postWithRetry("complete-download", body,
                requestHeaders("complete-download", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", "complete-download", response.statusCode(), response.body());
        }
    }

    /**
     * Fetches an object in a single request, handing the raw response back so callers can tell an
     * object that was too large (HTTP 413) from one that genuinely failed.
     *
     * @param bucketErn the ERN of the bucket holding the object
     * @param key the key of the object to fetch
     * @param maxInlineSize the largest object, in bytes, to accept in one response
     * @return the HTTP response, with the object's bytes - or a JSON error body - as its body
     * @throws IOException if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    private HttpResponse<byte[]> getObjectResponse(String bucketErn, String key, long maxInlineSize)
            throws IOException, InterruptedException {
        Map<String, String> headers = binaryRequestHeaders();
        headers.put("x-euclid-bucket-ern", bucketErn);
        headers.put("x-euclid-key", key);
        headers.put("x-euclid-part-size", Long.toString(maxInlineSize));
        return httpClient.postForBinary(baseUrl + "/", "esm", "get-object", headers);
    }

    /**
     * Sends an add-object-attribute or set-object-attribute action, which differ only in whether an
     * attribute of that name already on the object is overwritten.
     *
     * @param action the action to send, {@code "add-object-attribute"} or {@code "set-object-attribute"}
     * @param ern the ERN of the object carrying the attribute
     * @param name the attribute name
     * @param value the typed attribute value
     * @return an {@code ObjectAttributeResponse} carrying the attribute as the server stored it
     * @throws IOException if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    private ObjectAttributeResponse objectAttribute(String action, String ern, String name, Variant value)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ObjectAttributeRequest.builder().ern(ern).name(name).value(value).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", action,
                requestHeaders(action, body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("esm", action, response.statusCode(), response.body());
        }

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        JsonNode valueNode = root.get("value");
        return ObjectAttributeResponse.builder().ern(textOrNull(root, "ern")).name(textOrNull(root, "name"))
                .value(valueNode == null ? null : new Variant(textOrNull(valueNode, "type"), textOrNull(valueNode, "value")))
                .build();
    }

    /**
     * Builds the headers for the actions that send or receive raw bytes rather than JSON -
     * upload-part, put-object, get-object and download-part - where the request is described by
     * headers instead of a request body.
     * <p>
     * One deliberate deviation worth flagging: these always authenticate with the bearer token,
     * never SigV4, even when access keys are configured (unlike the rest of the SDK). Reason:
     * SigV4.sign() hashes the body as a UTF-8 String, which is lossy for arbitrary binary bytes;
     * and the reference EsmCli.cpp itself never SigV4-signs anything, only ever using the bearer
     * token - so bearer-only for these actions matches the CLI it's syncing against rather than
     * inventing new behavior.
     *
     * @return a mutable header map the caller adds its action-specific headers to
     */
    private Map<String, String> binaryRequestHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
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
        if (nameSpace != null && !nameSpace.isEmpty()) {
            headers.put("x-euclid-namespace", nameSpace);
        }
        return headers;
    }

    /**
     * Converts a JsonNode holding an array of subscriptions into a list of Subscription instances.
     *
     * @param subscriptionsNode the JsonNode representing the array of subscriptions
     * @return a list of Subscription instances, or an empty list if the node is null or not an array
     */
    private static List<Subscription> toSubscriptionList(JsonNode subscriptionsNode) {
        List<Subscription> subscriptions = new ArrayList<>();
        if (subscriptionsNode != null && subscriptionsNode.isArray()) {
            for (JsonNode subscriptionNode : subscriptionsNode) {
                subscriptions.add(new Subscription(
                        textOrNull(subscriptionNode, "ern"),
                        textOrNull(subscriptionNode, "sourceErn"),
                        textOrNull(subscriptionNode, "type"),
                        textOrNull(subscriptionNode, "targetErn"),
                        textOrNull(subscriptionNode, "created"),
                        textOrNull(subscriptionNode, "modified")));
            }
        }
        return subscriptions;
    }

    /**
     * Converts a given JsonNode into a map where keys are strings and values are Variant objects.
     *
     * @param node the JsonNode to convert, expected to be a JSON object of typed values
     * @return a map containing the JSON node's fields as keys and their corresponding Variant objects as values
     */
    private static Map<String, Variant> toVariantMap(JsonNode node) {
        Map<String, Variant> map = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                JsonNode valueNode = entry.getValue();
                map.put(entry.getKey(), new Variant(textOrNull(valueNode, "type"), textOrNull(valueNode, "value")));
            });
        }
        return map;
    }

    /**
     * Converts a JsonNode holding a JSON object of strings into a string-to-string map.
     *
     * @param node the JsonNode to convert
     * @return a map of the node's fields, or an empty map if the node is null or not an object
     */
    private static Map<String, String> toStringMap(JsonNode node) {
        Map<String, String> map = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(entry -> map.put(entry.getKey(), entry.getValue().asText()));
        }
        return map;
    }

    /**
     * Extracts and constructs a {@link CreateBucketResponse} object from the given JSON response body.
     *
     * @param responseBody the JSON response body as a string from which the bucket details are extracted.
     * @return a {@link CreateBucketResponse} object containing the extracted bucket details.
     * @throws IOException if there is an error processing the JSON response body.
     */
    private static CreateBucketResponse extractCreateBucketResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return CreateBucketResponse.builder().name(textOrNull(root, "name")).ern(textOrNull(root, "ern")).build();
    }

    /**
     * Extracts a GetBucketErnResponse object from the provided JSON response body.
     *
     * @param responseBody the JSON response body as a string
     * @return a GetBucketErnResponse object containing the extracted data
     * @throws IOException if there is an error parsing the response body
     */
    private static GetBucketErnResponse extractGetBucketErnResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return GetBucketErnResponse.builder().ern(textOrNull(root, "ern")).build();
    }

    /**
     * Extracts and constructs a {@code GetBucketSizeResponse} object from the provided JSON response body.
     *
     * @param responseBody the JSON response body as a string from which the bucket size response will be extracted.
     * @return an instance of {@code GetBucketSizeResponse} containing the extracted data.
     * @throws IOException if an error occurs while reading or parsing the JSON response body.
     */
    private static GetBucketSizeResponse extractGetBucketSizeResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return GetBucketSizeResponse.builder().ern(textOrNull(root, "ern")).size(root.path("size").asLong(0)).build();
    }

    /**
     * Extracts and constructs a PurgeBucketResponse object from the provided JSON response body.
     *
     * @param responseBody the JSON response body as a string
     * @return a PurgeBucketResponse object containing the extracted data
     * @throws IOException if an error occurs during parsing of the JSON response body
     */
    private static PurgeBucketResponse extractPurgeBucketResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return PurgeBucketResponse.builder().ern(textOrNull(root, "ern")).count(root.path("count").asLong(0)).build();
    }

    /**
     * Extracts a {@code CreateUploadResponse} object from the given JSON response body.
     *
     * @param responseBody the JSON response body as a string
     * @return a {@code CreateUploadResponse} object constructed from the extracted data
     * @throws IOException if an error occurs while processing the JSON response
     */
    private static CreateUploadResponse extractCreateUploadResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return CreateUploadResponse.builder().uploadId(textOrNull(root, "uploadId")).bucketErn(textOrNull(root, "bucketErn"))
                .key(textOrNull(root, "key")).build();
    }

    /**
     * Extracts and constructs a {@link CompleteUploadResponse} object from the given response body.
     *
     * @param responseBody the JSON response body as a string
     * @return a {@link CompleteUploadResponse} object populated with the data extracted from the response body
     * @throws IOException if an error occurs while parsing the JSON response
     */
    private static CompleteUploadResponse extractCompleteUploadResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return CompleteUploadResponse.builder().ern(textOrNull(root, "ern")).bucketErn(textOrNull(root, "bucketErn"))
                .key(textOrNull(root, "key")).size(root.path("size").asLong(0)).status(textOrNull(root, "status"))
                .contentType(textOrNull(root, "contentType")).md5Sum(textOrNull(root, "md5Sum")).build();
    }

    /**
     * Parses the given JSON response body and extracts the ListObjectsResponse.
     *
     * @param responseBody The JSON response body as a string.
     * @return A ListObjectsResponse object containing the parsed data.
     * @throws IOException If an error occurs while parsing the JSON response.
     */
    private static ListObjectsResponse extractListObjectsResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return ListObjectsResponse.builder().objects(toObjectList(root.get("objects")))
                .total(root.path("total").asLong(0)).build();
    }

    /**
     * Converts a JsonNode containing an array of objects into a list of EsmObject instances.
     *
     * @param objectsNode the JsonNode representing the array of objects; expected to be non-null and in array format.
     * @return a list of EsmObject instances constructed from the JsonNode, or an empty list if objectsNode is null or not an array.
     */
    private static List<EsmObject> toObjectList(JsonNode objectsNode) {
        List<EsmObject> objects = new ArrayList<>();
        if (objectsNode != null && objectsNode.isArray()) {
            for (JsonNode objectNode : objectsNode) {
                objects.add(toEsmObject(objectNode));
            }
        }
        return objects;
    }

    /**
     * Builds an {@link EsmObject} from one object's JSON, as it appears both inside a list-objects
     * array and on its own as the answer to copy/move/rename-object.
     *
     * @param objectNode the JSON object describing the stored object
     * @return the parsed object
     */
    private static EsmObject toEsmObject(JsonNode objectNode) {
        return new EsmObject(
                textOrNull(objectNode, "ern"),
                textOrNull(objectNode, "bucketErn"),
                textOrNull(objectNode, "key"),
                objectNode.path("size").asLong(0),
                textOrNull(objectNode, "status"),
                textOrNull(objectNode, "contentType"),
                textOrNull(objectNode, "md5Sum"),
                toVariantMap(objectNode.get("attributes")),
                textOrNull(objectNode, "created"),
                textOrNull(objectNode, "modified"));
    }

    /**
     * Extracts a list of bucket information from the provided JSON response body.
     *
     * @param responseBody the JSON response as a string containing the bucket information
     * @return a {@code ListBucketsResponse} extracted from the response body
     * @throws IOException if an error occurs while parsing the JSON response
     */
    private static ListBucketsResponse extractListBucketsResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode bucketsNode = root.get("buckets");
        List<Bucket> buckets = new ArrayList<>();
        if (bucketsNode != null && bucketsNode.isArray()) {
            for (JsonNode bucketNode : bucketsNode) {
                buckets.add(new Bucket(
                        textOrNull(bucketNode, "owner"),
                        textOrNull(bucketNode, "name"),
                        textOrNull(bucketNode, "ern"),
                        bucketNode.path("size").asLong(0),
                        bucketNode.path("objects").asLong(0),
                        toStringMap(bucketNode.get("tags")),
                        textOrNull(bucketNode, "created"),
                        textOrNull(bucketNode, "modified")));
            }
        }
        return ListBucketsResponse.builder().buckets(buckets).total(root.path("total").asLong(0)).build();
    }

    /**
     * Generates a map of request headers for a specified action and request body.
     * The headers include content type, region, account ID, user ID, and
     * authentication information. If AWS credentials are available, the headers
     * are signed using the SigV4 signing process; otherwise, a Bearer token is used.
     *
     * @param action the action being performed by the request.
     * @param body the body of the request to be included for signing.
     * @return a map containing all request headers required for the operation.
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
        if (nameSpace != null && !nameSpace.isEmpty()) {
            headers.put("x-euclid-namespace", nameSpace);
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

    /**
     * Generates the value of the "Host" HTTP header based on the URI's authority.
     * The generated value matches the format used by java.net.http to ensure
     * consistency between the signed value and the value sent to the server.
     *
     * @return The "Host" header value, consisting of the hostname optionally followed by a colon and port number.
     */
    private String hostHeader() {
        URI uri = URI.create(baseUrl);
        int port = uri.getPort();
        return port == -1 ? uri.getHost() : uri.getHost() + ":" + port;
    }

    /**
     * Retrieves the text value of a specified field from a JsonNode, or returns null if the field is missing or its value is null.
     *
     * @param node the JsonNode to extract the field value from
     * @param field the name of the field to retrieve
     * @return the text value of the specified field, or null if the field is missing or its value is null
     */
    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
