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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * EQS operations for this session. Requests are signed with SigV4 using
     * {@link #accessKeyId()}/{@link #secretAccessKey()} when both are present, falling back to
     * the bearer token otherwise - mirroring how euclid-cli authenticates service calls.
     */
    public EuclidEqs eqs() {
        return new EuclidEqs(baseUrl, token, region, accountId, userId, accessKeyId, secretAccessKey, caCertPath);
    }

    /**
     * ESM (storage) operations for this session. Requests are signed with SigV4 using
     * {@link #accessKeyId()}/{@link #secretAccessKey()} when both are present, falling back to
     * the bearer token otherwise - mirroring how euclid-cli authenticates service calls.
     */
    public EuclidEsm esm() {
        return new EuclidEsm(baseUrl, token, region, accountId, userId, accessKeyId, secretAccessKey, caCertPath);
    }

    public String getAccountId() {
        return accountId;
    }

    public String getRegion() {
        return region;
    }

    public List<User> listUsers() throws IOException, InterruptedException {
        return listUsers("", 10, 0, "userId");
    }

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

    public void deleteUser(String userId)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteUserRequest.builder().userId(userId).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "delete-user",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
    }

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

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
