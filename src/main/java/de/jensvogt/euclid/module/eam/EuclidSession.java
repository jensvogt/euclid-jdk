package de.jensvogt.euclid.module.eam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.dto.eam.DeleteUserRequest;
import de.jensvogt.euclid.dto.eam.ListUserRequest;
import de.jensvogt.euclid.dto.eam.ListUserResponse;
import de.jensvogt.euclid.dto.eam.RegisterRequest;
import de.jensvogt.euclid.exception.EuclidAuthenticationException;
import de.jensvogt.euclid.http.EuclidHttpClient;
import de.jensvogt.euclid.dto.eam.model.User;
import de.jensvogt.euclid.module.eqs.EuclidEqs;
import de.jensvogt.euclid.module.esm.EuclidEsm;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of a successful {@link EuclidEam#login()} call.
 *
 * @param token           the authentication token issued by the server
 * @param userId          the id of the authenticated user
 * @param accountId       the account the session is scoped to
 * @param region          the region the session is scoped to
 * @param accessKeyId     public identifier of the caller's SigV4 access key, e.g. "AKIA...".
 *                        Reused from an existing active key if the user already has one,
 *                        otherwise provisioned on login. Empty if the server didn't return one.
 * @param secretAccessKey secret used to sign requests, paired with accessKeyId. Empty if the
 *                        server didn't return one.
 * @param rawResponse     the raw JSON response body, for callers that need fields
 *                        beyond the ones above
 * @param baseUrl         the server this session was issued by, used for follow-up requests
 * @param caCertPath      path to an additional PEM CA certificate trusted for TLS connections to
 *                        {@code baseUrl}, or {@code null} to trust only the system store
 */
public record EuclidSession(String token, String userId, String accountId, String region, String accessKeyId,
                             String secretAccessKey, String rawResponse, String baseUrl, String caCertPath) {

    /**
     * A static and immutable instance of {@link ObjectMapper} used for JSON serialization
     * and deserialization within the {@code EuclidSession} class.
     *
     * This mapper is configured for generic-purpose JSON processing tasks and enables conversion
     * between Java objects and their JSON representations.
     *
     * Being a shared constant, this instance ensures consistent behavior and reduces the
     * overhead of repeatedly instantiating an {@link ObjectMapper}.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * EQS operations for this session. Requests are signed with SigV4 using
     * {@link #accessKeyId()}/{@link #secretAccessKey()} when both are present, falling back to
     * the bearer token otherwise - mirroring how euclid-cli authenticates service calls.
     *
     * @return EuclidEqs instance
     */
    public EuclidEqs eqs() {
        return new EuclidEqs(baseUrl, token, region, accountId, userId, accessKeyId, secretAccessKey, caCertPath);
    }

    /**
     * ESM (storage) operations for this session. Requests are signed with SigV4 using
     * {@link #accessKeyId()}/{@link #secretAccessKey()} when both are present, falling back to
     * the bearer token otherwise - mirroring how euclid-cli authenticates service calls.
     *
     * @return EuclidEsm instance
     */
    public EuclidEsm esm() {
        return new EuclidEsm(baseUrl, token, region, accountId, userId, accessKeyId, secretAccessKey, caCertPath);
    }

    /**
     * Retrieves the account ID associated with this session.
     *
     * @return the account ID as a {@code String}.
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Retrieves the region associated with this session.
     *
     * @return the region as a {@code String}.
     */
    public String getRegion() {
        return region;
    }

    /**
     * Retrieves a list of all available users using default filtering, pagination, and sorting parameters.
     *
     * @return a {@code List} of {@code User} objects.
     * @throws IOException           if an I/O error occurs during the operation.
     * @throws InterruptedException  if the operation is interrupted while waiting for a response.
     */
    public List<User> listUsers() throws IOException, InterruptedException {
        return listUsers("", 10, 0, "userId");
    }

    /**
     * Retrieves a list of users based on the provided filtering and pagination parameters.
     *
     * @param prefix      a string used to filter users whose identifiers start with the specified prefix
     * @param pageSize    the maximum number of users to retrieve per page
     * @param pageIndex   the index of the page to retrieve (zero-based)
     * @param sortColumn  the name of the column by which the user list should be sorted
     * @return a {@code List} of {@code User} objects matching the specified criteria
     * @throws IOException              if an I/O error occurs when sending or receiving the HTTP request
     * @throws InterruptedException     if the operation is interrupted while waiting for the HTTP response
     */
    public List<User> listUsers(String prefix, long pageSize, long pageIndex, String sortColumn)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListUserRequest.builder().prefix(prefix).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "list-users",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractListUserResponse(response.body()).users();
    }

    /**
     * Registers a new user in the system by sending an HTTP POST request with the user details.
     *
     * @param region    the region where the user is being registered
     * @param accountId the unique identifier of the account associated with the user
     * @param userId    the unique identifier of the user being registered
     * @param password  the password for the new user
     * @param email     the email address of the user being registered
     * @param isAdmin   indicates whether the user should have administrative privileges
     * @throws IOException          if an I/O error occurs during the registration process
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void register(String region, String accountId, String userId, String password, String email, boolean isAdmin)
            throws IOException, InterruptedException {
        RegisterRequest request = RegisterRequest.builder().region(region).accountId(accountId).userId(userId).password(password).email(email).isAdmin(isAdmin).build();
        String body = OBJECT_MAPPER.writeValueAsString(request);
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "register",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
    }

    /**
     * Deletes a user identified by the specified user ID.
     *
     * @param userId the unique identifier of the user to be deleted
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void deleteUser(String userId)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteUserRequest.builder().userId(userId).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "delete-user",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
    }

    /**
     * Merges the provided headers with additional session-specific headers like region, account ID, and user ID,
     * if they are available.
     *
     * @param headers a map of custom headers to be included in the request. These headers are merged
     *                with session-specific headers managed by the instance.
     * @return a map containing the combined headers, including the provided headers and any additional
     *         session-specific headers such as "x-euclid-region", "x-euclid-account-id", and "x-euclid-user-id".
     */
    private Map<String, String> requestHeaders(Map<String, String> headers) {
        Map<String, String> merged = new LinkedHashMap<>(headers);
        if (region != null) {
            merged.put("x-euclid-region", region);
        }
        if (accountId != null) {
            merged.put("x-euclid-account-id", accountId);
        }
        if (userId != null) {
            merged.put("x-euclid-user-id", userId);
        }
        return merged;
    }

    /**
     * Extracts and constructs a {@code ListUserResponse} object from a given JSON response string.
     *
     * @param responseBody the JSON response body as a {@code String}, containing user-related data.
     * @return a {@code ListUserResponse} object containing a list of {@code User} records
     *         and the total count of users.
     * @throws IOException if an error occurs while processing the JSON response.
     */
    private static ListUserResponse extractListUserResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode usersNode = root.get("users");
        List<User> users = new ArrayList<>();
        if (usersNode != null && usersNode.isArray()) {
            for (JsonNode userNode : usersNode) {
                users.add(new User(
                        textOrNull(userNode, "userId"),
                        textOrNull(userNode, "password"),
                        textOrNull(userNode, "email"),
                        textOrNull(userNode, "accountId"),
                        textOrNull(userNode, "region"),
                        userNode.path("isAdmin").asBoolean(false)));
            }
        }
        return ListUserResponse.builder().users(users).total(root.path("total").asLong(0)).build();
    }

    /**
     * Retrieves the text value of a specified field from a given {@code JsonNode}.
     * If the field is missing or has a null value, this method returns {@code null}.
     *
     * @param node  the {@code JsonNode} from which to extract the field value
     * @param field the name of the field to retrieve
     * @return the text value of the specified field as a {@code String}, or {@code null} if the field
     *         is missing or has a null value
     */
    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
