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

    /**
     * A singleton instance of {@code ObjectMapper} from the Jackson library used for
     * serializing Java objects to JSON and deserializing JSON to Java objects.
     * <br>
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
     *
     * The default value of 4 is chosen as a reasonable balance for environments with
     * multiple processor cores, allowing efficient multitasking without overwhelming
     * system resources.
     */
    private static final int DEFAULT_CONCURRENCY = 4;

    /**
     * The maximum number of retry attempts allowed for a single part upload operation.
     * This value determines how many times the system will retry in the case of a transient
     * failure before aborting the part upload.
     *
     * Designed to enhance the robustness of the upload process for large files, where
     * thousands of part uploads may occur. Allows a few quick retries to handle transient
     * issues, minimizing the risk of completely aborting the entire upload process.
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
     */
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
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
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
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
    }

    /**
     * Retrieves a list of storage buckets.
     *
     * This method fetches a list of buckets with default parameters: an
     * empty filter, a maximum of 10 results, an offset of 0, and ordering
     * by name.
     *
     * @return a list of Bucket objects representing the storage buckets.
     * @throws IOException if an I/O error occurs during the operation.
     * @throws InterruptedException if the operation is interrupted.
     */
    public List<Bucket> listBuckets() throws IOException, InterruptedException {
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
     * @return A list of Bucket objects matching the specified criteria.
     * @throws IOException            If an input or output exception occurs during the operation.
     * @throws InterruptedException   If the calling thread is interrupted.
     */
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
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
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
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
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
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
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
        String body = OBJECT_MAPPER.writeValueAsString(PurgeBucketRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "purge-bucket",
                requestHeaders("purge-bucket", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractPurgeBucketResponse(response.body());
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
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "create-upload", headers);

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractCreateUploadResponse(response.body());
    }

    /**
     * Uploads a single part of a multipart upload. This method is used internally by 
     * the uploadFile functionality and is not intended to be used as a standalone action.
     * Data is sent as raw bytes directly over the wire without including a JSON body.
     * The authentication for this method uses the bearer token instead of SigV4 signing.
     *
     * Uploads one part of a multipart upload (internal helper used by uploadFile; not a
     * standalone action). Unlike every other action here, this does NOT send a JSON body -
     * uploadId/partNumber ride as headers and data goes straight over the wire as raw bytes.
     *
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
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "esm", "complete-upload",
                requestHeaders("complete-upload", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractCompleteUploadResponse(response.body());
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

    /**
     * Extracts a list of bucket information from the provided JSON response body.
     *
     * @param responseBody the JSON response as a string containing the bucket information
     * @return a list of {@code Bucket} objects extracted from the response body
     * @throws IOException if an error occurs while parsing the JSON response
     */
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
