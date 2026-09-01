package de.jensvogt.euclid.module.eam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.dto.Metadata;
import de.jensvogt.euclid.dto.eam.ChangeNamespaceRequest;
import de.jensvogt.euclid.dto.eam.CreateAccessKeyResponse;
import de.jensvogt.euclid.dto.eam.CreateAccountRequest;
import de.jensvogt.euclid.dto.eam.CreateNamespaceRequest;
import de.jensvogt.euclid.dto.eam.CreateUserGroupRequest;
import de.jensvogt.euclid.dto.eam.DeleteAccessKeyRequest;
import de.jensvogt.euclid.dto.eam.DeleteAccountRequest;
import de.jensvogt.euclid.dto.eam.DeleteNamespaceRequest;
import de.jensvogt.euclid.dto.eam.DeleteUserGroupRequest;
import de.jensvogt.euclid.dto.eam.DeleteUserRequest;
import de.jensvogt.euclid.dto.eam.GrantNamespaceAccessRequest;
import de.jensvogt.euclid.dto.eam.ListAccountsRequest;
import de.jensvogt.euclid.dto.eam.ListAccountsResponse;
import de.jensvogt.euclid.dto.eam.ListNamespacesRequest;
import de.jensvogt.euclid.dto.eam.ListNamespacesResponse;
import de.jensvogt.euclid.dto.eam.ListUserGroupsRequest;
import de.jensvogt.euclid.dto.eam.ListUserGroupsResponse;
import de.jensvogt.euclid.dto.eam.ListUserRequest;
import de.jensvogt.euclid.dto.eam.ListUserResponse;
import de.jensvogt.euclid.dto.eam.RegisterRequest;
import de.jensvogt.euclid.dto.eam.RevokeNamespaceAccessRequest;
import de.jensvogt.euclid.dto.eam.UserGroupAddUserRequest;
import de.jensvogt.euclid.dto.eam.UserGroupRemoveUserRequest;
import de.jensvogt.euclid.exception.EuclidServiceException;
import de.jensvogt.euclid.http.EuclidHttpClient;
import de.jensvogt.euclid.dto.eam.model.Account;
import de.jensvogt.euclid.dto.eam.model.AccessKey;
import de.jensvogt.euclid.dto.eam.model.AccountGrant;
import de.jensvogt.euclid.dto.eam.model.Namespace;
import de.jensvogt.euclid.dto.eam.model.User;
import de.jensvogt.euclid.dto.eam.model.UserGroup;
import de.jensvogt.euclid.module.ekm.EuclidEkm;
import de.jensvogt.euclid.module.ens.EuclidEns;
import de.jensvogt.euclid.module.eqs.EuclidEqs;
import de.jensvogt.euclid.module.esm.EuclidEsm;
import de.jensvogt.euclid.module.ets.EuclidEts;

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
 * @param isAdmin         whether the logged-in user has administrator privileges. Carried alongside
 *                        the token - as euclid-cli does - so admin-only work can be turned away
 *                        locally without a round trip, though the server enforces this independently
 *                        regardless.
 * @param rawResponse     the raw JSON response body, for callers that need fields
 *                        beyond the ones above
 * @param baseUrl         the server this session was issued by, used for follow-up requests
 * @param caCertPath      path to an additional PEM CA certificate trusted for TLS connections to
 *                        {@code baseUrl}, or {@code null} to trust only the system store
 * @param nameSpace       the session's active namespace, or {@code null} if unscoped. Set via
 *                        {@link EuclidEam#namespace(String)} at login or {@link #changeNamespace(String)}
 *                        afterward; sent as the {@code x-euclid-namespace} header on every
 *                        subsequent request from this session, mirroring euclid-cli's HttpClient.cpp
 */
public record EuclidSession(String token, String userId, String accountId, String region, String accessKeyId,
                            String secretAccessKey, boolean isAdmin, String rawResponse, String baseUrl,
                            String caCertPath, String nameSpace) {

    /**
     * A static and immutable instance of {@link ObjectMapper} used for JSON serialization
     * and deserialization within the {@code EuclidSession} class.
     * <p>
     * This mapper is configured for generic-purpose JSON processing tasks and enables conversion
     * between Java objects and their JSON representations.
     * <p>
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
        return new EuclidEqs(baseUrl, token, region, accountId, userId, accessKeyId, secretAccessKey, caCertPath, nameSpace);
    }

    /**
     * ESM (storage) operations for this session. Requests are signed with SigV4 using
     * {@link #accessKeyId()}/{@link #secretAccessKey()} when both are present, falling back to
     * the bearer token otherwise - mirroring how euclid-cli authenticates service calls.
     *
     * @return EuclidEsm instance
     */
    public EuclidEsm esm() {
        return new EuclidEsm(baseUrl, token, region, accountId, userId, accessKeyId, secretAccessKey, caCertPath, nameSpace);
    }

    /**
     * ENS (pub/sub topic) operations for this session. Requests are signed with SigV4 using
     * {@link #accessKeyId()}/{@link #secretAccessKey()} when both are present, falling back to
     * the bearer token otherwise - mirroring how euclid-cli authenticates service calls.
     *
     * @return EuclidEns instance
     */
    public EuclidEns ens() {
        return new EuclidEns(baseUrl, token, region, accountId, userId, accessKeyId, secretAccessKey, caCertPath, nameSpace);
    }

    /**
     * EKM (key management) operations for this session. Requests are signed with SigV4 using
     * {@link #accessKeyId()}/{@link #secretAccessKey()} when both are present, falling back to
     * the bearer token otherwise - mirroring how euclid-cli authenticates service calls.
     *
     * @return EuclidEkm instance
     */
    public EuclidEkm ekm() {
        return new EuclidEkm(baseUrl, token, region, accountId, userId, accessKeyId, secretAccessKey, caCertPath, nameSpace);
    }

    /**
     * ETS (transfer server) operations for this session. Requests are signed with SigV4 using
     * {@link #accessKeyId()}/{@link #secretAccessKey()} when both are present, falling back to
     * the bearer token otherwise - mirroring how euclid-cli authenticates service calls.
     * <p>
     * Every ETS action is administrator-only server-side, so a session whose {@link #isAdmin()} is
     * false gets HTTP 403 from all of them.
     *
     * @return EuclidEts instance
     */
    public EuclidEts ets() {
        return new EuclidEts(baseUrl, token, region, accountId, userId, accessKeyId, secretAccessKey, caCertPath, nameSpace);
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
     * @return a {@code ListUserResponse} carrying the users and how many exist in total.
     * @throws IOException           if an I/O error occurs during the operation.
     * @throws InterruptedException  if the operation is interrupted while waiting for a response.
     */
    public ListUserResponse listUsers() throws IOException, InterruptedException {
        return listUsers("", 10, 0, "userId");
    }

    /**
     * Retrieves a list of users based on the provided filtering and pagination parameters.
     *
     * @param prefix      a string used to filter users whose identifiers start with the specified prefix
     * @param pageSize    the maximum number of users to retrieve per page
     * @param pageIndex   the index of the page to retrieve (zero-based)
     * @param sortColumn  the name of the column by which the user list should be sorted
     * @return a {@code ListUserResponse} carrying the matching users and their total
     * @throws IOException              if an I/O error occurs when sending or receiving the HTTP request
     * @throws InterruptedException     if the operation is interrupted while waiting for the HTTP response
     */
    public ListUserResponse listUsers(String prefix, long pageSize, long pageIndex, String sortColumn)
            throws IOException, InterruptedException {
        return listUsers(prefix, pageSize, pageIndex, sortColumn, "asc");
    }

    /**
     * Retrieves a list of users based on the provided filtering and pagination parameters, in a
     * chosen sort direction.
     *
     * @param prefix        a string used to filter users whose identifiers start with the specified prefix
     * @param pageSize      the maximum number of users to retrieve per page
     * @param pageIndex     the index of the page to retrieve (zero-based)
     * @param sortColumn    the name of the column by which the user list should be sorted
     * @param sortDirection sort direction, {@code "asc"} or {@code "desc"}
     * @return a {@code ListUserResponse} carrying the matching users and their total
     * @throws IOException              if an I/O error occurs when sending or receiving the HTTP request
     * @throws InterruptedException     if the operation is interrupted while waiting for the HTTP response
     */
    public ListUserResponse listUsers(String prefix, long pageSize, long pageIndex, String sortColumn,
                                      String sortDirection) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListUserRequest.builder().prefix(prefix).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).sortDirection(sortDirection).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "list-users",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "list-users", response.statusCode(), response.body());
        }

        return extractListUserResponse(response.body());
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
            throw new EuclidServiceException("eam", "register", response.statusCode(), response.body());
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
            throw new EuclidServiceException("eam", "delete-user", response.statusCode(), response.body());
        }
    }

    /**
     * Switches the active namespace for this session: validated by the server against the
     * current account (and the caller's namespace grants, unless an account administrator) and,
     * on success, persisted to the local credentials cache alongside the returned session -
     * mirroring euclid-cli's "eam change-namespace" action. Every namespace-scoped call made
     * through the returned session is automatically restricted to it, until changed again.
     *
     * @param namespace the namespace to switch to; empty clears it back to unscoped
     * @return a new session, identical to this one except with the namespace switched
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public EuclidSession changeNamespace(String namespace) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(ChangeNamespaceRequest.builder().namespace(namespace).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "change-namespace",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "change-namespace", response.statusCode(), response.body());
        }

        EuclidSession updated = new EuclidSession(token, userId, accountId, region, accessKeyId, secretAccessKey,
                isAdmin, rawResponse, baseUrl, caCertPath, namespace);
        EuclidEam.updateCachedNamespace(baseUrl, namespace);
        return updated;
    }

    /**
     * Creates a new SigV4 access key for this session's user and returns it. The secret is only
     * ever returned once, right here - it is not retrievable again via {@link #listAccessKeys()}.
     *
     * @return the newly created access key, including its secret
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public CreateAccessKeyResponse createAccessKey() throws IOException, InterruptedException {
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", "{}", "eam", "create-access-key",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "create-access-key", response.statusCode(), response.body());
        }

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        return CreateAccessKeyResponse.builder().metadata(toMetadata(root.get("metadata")))
                .accessKeyId(textOrNull(root, "accessKeyId")).secretAccessKey(textOrNull(root, "secretAccessKey"))
                .createdAt(textOrNull(root, "createdAt")).build();
    }

    /**
     * Lists this session's user's own access keys. Secrets are never returned after creation.
     *
     * @return the caller's access keys
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public List<AccessKey> listAccessKeys() throws IOException, InterruptedException {
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", "{}", "eam", "list-access-keys",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "list-access-keys", response.statusCode(), response.body());
        }

        List<AccessKey> keys = new ArrayList<>();
        JsonNode node = OBJECT_MAPPER.readTree(response.body()).get("accessKeys");
        if (node != null && node.isArray()) {
            for (JsonNode keyNode : node) {
                keys.add(new AccessKey(textOrNull(keyNode, "accessKeyId"), keyNode.path("active").asBoolean(true),
                        textOrNull(keyNode, "createdAt")));
            }
        }
        return keys;
    }

    /**
     * Deletes one of this session's user's own access keys.
     *
     * @param accessKeyId the access key ID to delete, e.g. "AKIA..."
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void deleteAccessKey(String accessKeyId) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteAccessKeyRequest.builder().accessKeyId(accessKeyId).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "delete-access-key",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "delete-access-key", response.statusCode(), response.body());
        }
    }

    /**
     * Creates a new, empty user group. Requires administrator privileges.
     *
     * @param name        group name, unique across the deployment
     * @param description free-text description of the group's purpose
     * @return the newly created group
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public UserGroup createUserGroup(String name, String description) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                CreateUserGroupRequest.builder().name(name).description(description).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "create-user-group",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "create-user-group", response.statusCode(), response.body());
        }

        return toUserGroup(OBJECT_MAPPER.readTree(response.body()).get("userGroup"));
    }

    /**
     * Lists user groups using default filtering, pagination, and sorting parameters.
     *
     * @return the user groups
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public ListUserGroupsResponse listUserGroups() throws IOException, InterruptedException {
        return listUserGroups("", 10, 0, "userId");
    }

    /**
     * Lists user groups, optionally filtered by name prefix and paginated.
     *
     * @param prefix     user group name prefix
     * @param pageSize   page size
     * @param pageIndex  page index (zero-based)
     * @param sortColumn sorting column
     * @return a {@code ListUserGroupsResponse} carrying the matching user groups and their total
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public ListUserGroupsResponse listUserGroups(String prefix, long pageSize, long pageIndex, String sortColumn)
            throws IOException, InterruptedException {
        return listUserGroups(prefix, pageSize, pageIndex, sortColumn, "asc");
    }

    /**
     * Lists user groups, optionally filtered by name prefix and paginated, in a chosen sort direction.
     *
     * @param prefix        user group name prefix
     * @param pageSize      page size
     * @param pageIndex     page index (zero-based)
     * @param sortColumn    sorting column
     * @param sortDirection sort direction, {@code "asc"} or {@code "desc"}
     * @return a {@code ListUserGroupsResponse} carrying the matching user groups and their total
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public ListUserGroupsResponse listUserGroups(String prefix, long pageSize, long pageIndex, String sortColumn,
                                          String sortDirection) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListUserGroupsRequest.builder().prefix(prefix).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).sortDirection(sortDirection).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "list-user-groups",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "list-user-groups", response.statusCode(), response.body());
        }

        List<UserGroup> groups = new ArrayList<>();
        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        JsonNode node = root.get("userGroups");
        if (node != null && node.isArray()) {
            for (JsonNode groupNode : node) {
                groups.add(toUserGroup(groupNode));
            }
        }
        return ListUserGroupsResponse.builder().userGroups(groups).total(root.path("total").asLong(0)).build();
    }

    /**
     * Adds a user to a user group.
     *
     * @param userGroup user group ERN
     * @param user      user ERN
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void addUserToUserGroup(String userGroup, String user) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                UserGroupAddUserRequest.builder().userGroup(userGroup).user(user).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "user-group-add-user",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "user-group-add-user", response.statusCode(), response.body());
        }
    }

    /**
     * Removes a user from a user group.
     *
     * @param userGroup user group ERN
     * @param user      user ERN
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void removeUserFromUserGroup(String userGroup, String user) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                UserGroupRemoveUserRequest.builder().userGroup(userGroup).user(user).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "user-group-remove-user",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "user-group-remove-user", response.statusCode(), response.body());
        }
    }

    /**
     * Deletes an existing user group. Requires administrator privileges.
     *
     * @param name group name to delete
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void deleteUserGroup(String name) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteUserGroupRequest.builder().name(name).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "delete-user-group",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "delete-user-group", response.statusCode(), response.body());
        }
    }

    /**
     * Creates a new account. Requires administrator privileges.
     *
     * @param accountId   account ID, unique across the deployment
     * @param name        human-readable account name
     * @param description free-text description of the account's purpose
     * @return the newly created account
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public Account createAccount(String accountId, String name, String description) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                CreateAccountRequest.builder().accountId(accountId).name(name).description(description).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "create-account",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "create-account", response.statusCode(), response.body());
        }

        return toAccount(OBJECT_MAPPER.readTree(response.body()).get("account"));
    }

    /**
     * Lists accounts using default filtering, pagination, and sorting parameters.
     *
     * @return the accounts
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public ListAccountsResponse listAccounts() throws IOException, InterruptedException {
        return listAccounts("", 10, 0, "accountId");
    }

    /**
     * Lists accounts, optionally filtered by accountId prefix and paginated.
     *
     * @param prefix     account ID prefix
     * @param pageSize   page size
     * @param pageIndex  page index (zero-based)
     * @param sortColumn sorting column
     * @return a {@code ListAccountsResponse} carrying the matching accounts and their total
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public ListAccountsResponse listAccounts(String prefix, long pageSize, long pageIndex, String sortColumn)
            throws IOException, InterruptedException {
        return listAccounts(prefix, pageSize, pageIndex, sortColumn, "asc");
    }

    /**
     * Lists accounts, optionally filtered by accountId prefix and paginated, in a chosen sort direction.
     *
     * @param prefix        account ID prefix
     * @param pageSize      page size
     * @param pageIndex     page index (zero-based)
     * @param sortColumn    sorting column
     * @param sortDirection sort direction, {@code "asc"} or {@code "desc"}
     * @return a {@code ListAccountsResponse} carrying the matching accounts and their total
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public ListAccountsResponse listAccounts(String prefix, long pageSize, long pageIndex, String sortColumn,
                                      String sortDirection) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListAccountsRequest.builder().prefix(prefix).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).sortDirection(sortDirection).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "list-accounts",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "list-accounts", response.statusCode(), response.body());
        }

        List<Account> accounts = new ArrayList<>();
        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        JsonNode node = root.get("accounts");
        if (node != null && node.isArray()) {
            for (JsonNode accountNode : node) {
                accounts.add(toAccount(accountNode));
            }
        }
        return ListAccountsResponse.builder().accounts(accounts).total(root.path("total").asLong(0)).build();
    }

    /**
     * Deletes an existing account. Requires administrator privileges, and the account must have
     * no remaining namespaces or user grants.
     *
     * @param accountId account ID to delete
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void deleteAccount(String accountId) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteAccountRequest.builder().accountId(accountId).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "delete-account",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "delete-account", response.statusCode(), response.body());
        }
    }

    /**
     * Creates a new namespace under an account. Requires administrator privileges on that account.
     *
     * @param accountId   the account the namespace belongs to
     * @param name        namespace name, unique within accountId
     * @param description free-text description of the namespace's purpose
     * @return the newly created namespace
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public Namespace createNamespace(String accountId, String name, String description) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                CreateNamespaceRequest.builder().accountId(accountId).name(name).description(description).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "create-namespace",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "create-namespace", response.statusCode(), response.body());
        }

        return toNamespace(OBJECT_MAPPER.readTree(response.body()).get("namespace"));
    }

    /**
     * Lists namespaces under an account using default filtering, pagination, and sorting parameters.
     *
     * @param accountId only namespaces belonging to this account are returned
     * @return the namespaces
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public ListNamespacesResponse listNamespaces(String accountId) throws IOException, InterruptedException {
        return listNamespaces(accountId, "", 10, 0, "name");
    }

    /**
     * Lists namespaces under an account, optionally filtered by name prefix and paginated.
     *
     * @param accountId  only namespaces belonging to this account are returned
     * @param prefix     namespace name prefix
     * @param pageSize   page size
     * @param pageIndex  page index (zero-based)
     * @param sortColumn sorting column
     * @return a {@code ListNamespacesResponse} carrying the matching namespaces and their total
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public ListNamespacesResponse listNamespaces(String accountId, String prefix, long pageSize, long pageIndex,
                                                 String sortColumn)
            throws IOException, InterruptedException {
        return listNamespaces(accountId, prefix, pageSize, pageIndex, sortColumn, "asc");
    }

    /**
     * Lists the namespaces of an account, optionally filtered by name prefix and paginated, in a
     * chosen sort direction.
     *
     * @param accountId     only namespaces belonging to this account are returned
     * @param prefix        namespace name prefix
     * @param pageSize      page size
     * @param pageIndex     page index (zero-based)
     * @param sortColumn    sorting column
     * @param sortDirection sort direction, {@code "asc"} or {@code "desc"}
     * @return a {@code ListNamespacesResponse} carrying the matching namespaces and their total
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public ListNamespacesResponse listNamespaces(String accountId, String prefix, long pageSize, long pageIndex,
                                          String sortColumn, String sortDirection)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListNamespacesRequest.builder().accountId(accountId).prefix(prefix).pageSize(pageSize)
                        .pageIndex(pageIndex).sortColumn(sortColumn).sortDirection(sortDirection).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "list-namespaces",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "list-namespaces", response.statusCode(), response.body());
        }

        List<Namespace> namespaces = new ArrayList<>();
        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        JsonNode node = root.get("namespaces");
        if (node != null && node.isArray()) {
            for (JsonNode namespaceNode : node) {
                namespaces.add(toNamespace(namespaceNode));
            }
        }
        return ListNamespacesResponse.builder().namespaces(namespaces).total(root.path("total").asLong(0)).build();
    }

    /**
     * Deletes an existing namespace. Requires administrator privileges on the account, and the
     * namespace must have no remaining user grants.
     *
     * @param accountId the account the namespace belongs to
     * @param name      namespace name to delete
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void deleteNamespace(String accountId, String name) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                DeleteNamespaceRequest.builder().accountId(accountId).name(name).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "delete-namespace",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "delete-namespace", response.statusCode(), response.body());
        }
    }

    /**
     * Grants a user access to a namespace within an account. Requires administrator privileges
     * on that account.
     *
     * @param user      user ERN to grant access to
     * @param accountId the account the namespace belongs to
     * @param namespace namespace within accountId to grant access to
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void grantNamespaceAccess(String user, String accountId, String namespace) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                GrantNamespaceAccessRequest.builder().user(user).accountId(accountId).namespace(namespace).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "grant-namespace-access",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "grant-namespace-access", response.statusCode(), response.body());
        }
    }

    /**
     * Revokes a user's access to a namespace within an account. Requires administrator privileges
     * on that account.
     *
     * @param user      user ERN to revoke access from
     * @param accountId the account the namespace belongs to
     * @param namespace namespace within accountId to revoke access from
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public void revokeNamespaceAccess(String user, String accountId, String namespace) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                RevokeNamespaceAccessRequest.builder().user(user).accountId(accountId).namespace(namespace).build());
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + "/", body, "eam", "revoke-namespace-access",
                requestHeaders(Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token)));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "revoke-namespace-access", response.statusCode(), response.body());
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
        if (nameSpace != null && !nameSpace.isEmpty()) {
            merged.put("x-euclid-namespace", nameSpace);
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
                users.add(toUser(userNode));
            }
        }
        return ListUserResponse.builder().users(users).total(root.path("total").asLong(0)).build();
    }

    /**
     * Builds a {@code User} from its JSON representation.
     *
     * @param userNode the user's JSON representation
     * @return the parsed {@code User}
     */
    private static User toUser(JsonNode userNode) {
        return new User(
                textOrNull(userNode, "userId"),
                textOrNull(userNode, "ern"),
                textOrNull(userNode, "password"),
                textOrNull(userNode, "email"),
                textOrNull(userNode, "accountId"),
                textOrNull(userNode, "region"),
                toAccountGrantList(userNode.get("accountGrants")),
                textOrNull(userNode, "created"),
                textOrNull(userNode, "modified"));
    }

    /**
     * Builds the list of {@code AccountGrant}s from its JSON representation.
     *
     * @param accountGrantsNode the account grants' JSON representation, or {@code null} if absent
     * @return the parsed {@code AccountGrant} list, empty if {@code accountGrantsNode} is absent
     */
    private static List<AccountGrant> toAccountGrantList(JsonNode accountGrantsNode) {
        List<AccountGrant> grants = new ArrayList<>();
        if (accountGrantsNode != null && accountGrantsNode.isArray()) {
            for (JsonNode grantNode : accountGrantsNode) {
                List<String> namespaces = new ArrayList<>();
                JsonNode namespacesNode = grantNode.get("namespaces");
                if (namespacesNode != null && namespacesNode.isArray()) {
                    for (JsonNode nsNode : namespacesNode) {
                        namespaces.add(nsNode.asText());
                    }
                }
                grants.add(new AccountGrant(textOrNull(grantNode, "accountId"), namespaces,
                        grantNode.path("isAdmin").asBoolean(false), textOrNull(grantNode, "granted")));
            }
        }
        return grants;
    }

    /**
     * Builds a {@code UserGroup} from its JSON representation.
     *
     * @param node the group's JSON representation, or {@code null}
     * @return the parsed {@code UserGroup}, or {@code null} if {@code node} is {@code null}
     */
    private static UserGroup toUserGroup(JsonNode node) {
        if (node == null) {
            return null;
        }
        List<String> userIds = new ArrayList<>();
        JsonNode userIdsNode = node.get("userIds");
        if (userIdsNode != null && userIdsNode.isArray()) {
            for (JsonNode userIdNode : userIdsNode) {
                userIds.add(userIdNode.asText());
            }
        }
        return new UserGroup(textOrNull(node, "name"), textOrNull(node, "ern"), textOrNull(node, "accountId"),
                textOrNull(node, "region"), textOrNull(node, "description"), userIds,
                textOrNull(node, "created"), textOrNull(node, "modified"));
    }

    /**
     * Builds an {@code Account} from its JSON representation.
     *
     * @param node the account's JSON representation, or {@code null}
     * @return the parsed {@code Account}, or {@code null} if {@code node} is {@code null}
     */
    private static Account toAccount(JsonNode node) {
        if (node == null) {
            return null;
        }
        return new Account(textOrNull(node, "accountId"), textOrNull(node, "name"), textOrNull(node, "ern"),
                textOrNull(node, "description"), textOrNull(node, "created"), textOrNull(node, "modified"));
    }

    /**
     * Builds a {@code Namespace} from its JSON representation.
     *
     * @param node the namespace's JSON representation, or {@code null}
     * @return the parsed {@code Namespace}, or {@code null} if {@code node} is {@code null}
     */
    private static Namespace toNamespace(JsonNode node) {
        if (node == null) {
            return null;
        }
        return new Namespace(textOrNull(node, "accountId"), textOrNull(node, "name"), textOrNull(node, "ern"),
                textOrNull(node, "description"), textOrNull(node, "created"), textOrNull(node, "modified"));
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

    /**
     * Reads the nested {@code "metadata"} object a response carries the caller's identity in.
     *
     * @param node the metadata node, or {@code null} if the response has none
     * @return the parsed metadata, or {@code null} if there was no metadata object
     */
    private static Metadata toMetadata(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        return new Metadata(textOrNull(node, "region"), textOrNull(node, "accountId"), textOrNull(node, "user"));
    }
}
