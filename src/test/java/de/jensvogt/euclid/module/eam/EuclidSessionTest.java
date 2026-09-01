package de.jensvogt.euclid.module.eam;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.dto.eam.CreateAccessKeyResponse;
import de.jensvogt.euclid.dto.eam.ListAccountsResponse;
import de.jensvogt.euclid.dto.eam.ListNamespacesResponse;
import de.jensvogt.euclid.dto.eam.ListUserGroupsResponse;
import de.jensvogt.euclid.dto.eam.ListUserResponse;
import de.jensvogt.euclid.dto.eam.model.AccessKey;
import de.jensvogt.euclid.dto.eam.model.Account;
import de.jensvogt.euclid.dto.eam.model.Namespace;
import de.jensvogt.euclid.dto.eam.model.User;
import de.jensvogt.euclid.dto.eam.model.UserGroup;
import de.jensvogt.euclid.exception.EuclidServiceException;
import de.jensvogt.euclid.module.ens.EuclidEns;
import de.jensvogt.euclid.module.eqs.EuclidEqs;
import de.jensvogt.euclid.module.esm.EuclidEsm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers every {@link EuclidSession} action: routes to the right eam action with a
 * correctly-shaped request body, parses the response, surfaces non-2xx responses as
 * {@link EuclidServiceException}, and sends {@code x-euclid-namespace} once a namespace
 * is active - mirroring euclid-cli's EamCli.cpp.
 */
class EuclidSessionTest {

    private static final List<String> HEADERS_TO_CAPTURE = List.of("x-euclid-action", "x-euclid-target",
            "x-euclid-region", "x-euclid-account-id", "x-euclid-user-id", "x-euclid-namespace", "Authorization");

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void listUsersUsesDefaultsAndParsesResponse() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{\"users\":[" + userJson("bob") + "],\"total\":1}");
        });

        ListUserResponse response = newSession().listUsers();
        List<User> users = response.users();
        assertEquals(1, response.total(), "the server's total must survive rather than be dropped");

        assertEquals("list-users", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"prefix\":\"\"", "\"pageSize\":10", "\"pageIndex\":0",
                "\"sortColumn\":\"userId\"");
        assertEquals(1, users.size());
        User user = users.getFirst();
        assertEquals("bob", user.userId());
        assertEquals("user-ern", user.ern());
        assertEquals(1, user.accountGrants().size());
        assertEquals("863459426936", user.accountGrants().get(0).accountId());
        assertTrue(user.accountGrants().getFirst().isAdmin());
    }

    @Test
    void listUsersWithExplicitParameters() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{\"users\":[],\"total\":0}");
        });

        newSession().listUsers("bo", 25, 2, "email");

        assertBodyContains(received.get().body(), "\"prefix\":\"bo\"", "\"pageSize\":25", "\"pageIndex\":2",
                "\"sortColumn\":\"email\"");
    }

    @Test
    void registerSendsUserDetails() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newSession().register("eu-central-1", "863459426936", "bob", "s3cret", "bob@example.com", true);

        assertEquals("register", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"region\":\"eu-central-1\"", "\"accountId\":\"863459426936\"",
                "\"userId\":\"bob\"", "\"password\":\"s3cret\"", "\"email\":\"bob@example.com\"", "\"isAdmin\":true");
    }

    @Test
    void deleteUserSendsUserId() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newSession().deleteUser("bob");

        assertEquals("delete-user", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"userId\":\"bob\"");
    }

    @Test
    void changeNamespaceUpdatesSessionAndSendsBody() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{}");
        });

        EuclidSession session = newSession();
        EuclidSession updated = session.changeNamespace("prod");

        assertEquals("change-namespace", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"namespace\":\"prod\"");
        assertNull(session.nameSpace(), "the original session must stay unchanged - EuclidSession is immutable");
        assertEquals("prod", updated.nameSpace());
    }

    @Test
    void namespaceScopedSessionSendsNamespaceHeaderOnSubsequentCalls() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newSessionWithNamespace("prod").deleteUser("bob");

        assertEquals("prod", received.get().header("x-euclid-namespace"));
    }

    @Test
    void eqsEsmEnsInheritTheSessionsNamespace() throws Exception {
        // Regression test: session.eqs()/.esm()/.ens() must thread the session's active
        // namespace through to the client they build - otherwise every queue/bucket/topic
        // created through them silently lands in the unnamed/default namespace regardless of
        // what namespace the session was scoped to at login.
        AtomicReference<String> eqsNamespaceHeader = new AtomicReference<>();
        AtomicReference<String> esmNamespaceHeader = new AtomicReference<>();
        AtomicReference<String> ensNamespaceHeader = new AtomicReference<>();
        server = startServer(exchange -> {
            switch (exchange.getRequestHeaders().getFirst("x-euclid-target")) {
                case "eqs" -> eqsNamespaceHeader.set(exchange.getRequestHeaders().getFirst("x-euclid-namespace"));
                case "esm" -> esmNamespaceHeader.set(exchange.getRequestHeaders().getFirst("x-euclid-namespace"));
                case "ens" -> ensNamespaceHeader.set(exchange.getRequestHeaders().getFirst("x-euclid-namespace"));
                default -> throw new IllegalStateException("unexpected target");
            }
            sendResponse(exchange, 200, "{}");
        });

        EuclidSession session = newSessionWithNamespace("prod");
        EuclidEqs eqs = session.eqs();
        EuclidEsm esm = session.esm();
        EuclidEns ens = session.ens();
        eqs.deleteQueue("queue-ern");
        esm.deleteBucket("bucket-ern");
        ens.deleteTopic("topic-ern");

        assertEquals("prod", eqsNamespaceHeader.get());
        assertEquals("prod", esmNamespaceHeader.get());
        assertEquals("prod", ensNamespaceHeader.get());
    }

    @Test
    void createAccessKeyParsesResponse() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{\"accessKeyId\":\"AKIDEXAMPLE\",\"secretAccessKey\":\"s3cr3t\",\"createdAt\":\"2026-01-01\"}");
        });

        CreateAccessKeyResponse response = newSession().createAccessKey();

        assertEquals("create-access-key", received.get().header("x-euclid-action"));
        assertEquals("AKIDEXAMPLE", response.accessKeyId());
        assertEquals("s3cr3t", response.secretAccessKey());
    }

    // The caller identity the server resolved the request to travels in a nested "metadata" object
    // so a response DTO's own fields stay at the top level.
    @Test
    void createAccessKeyParsesTheResponseMetadata() throws Exception {
        server = startServer(exchange -> {
            capture(exchange);
            sendResponse(exchange, 200, "{\"metadata\":{\"region\":\"eu-central-1\",\"accountId\":\"863459426936\","
                    + "\"user\":\"alice\"},\"accessKeyId\":\"AKIDEXAMPLE\",\"secretAccessKey\":\"s3cr3t\","
                    + "\"createdAt\":\"2026-01-01\"}");
        });

        CreateAccessKeyResponse response = newSession().createAccessKey();

        assertEquals("eu-central-1", response.metadata().region());
        assertEquals("863459426936", response.metadata().accountId());
        assertEquals("alice", response.metadata().user());
    }

    @Test
    void listRequestsCarrySortDirection() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"total\":0,\"accounts\":[],\"namespaces\":[],\"userGroups\":[]}");
        });

        EuclidSession session = newSession();
        session.listAccounts("", 10, 0, "name", "desc");
        session.listNamespaces("863459426936", "", 10, 0, "name", "desc");
        session.listUserGroups("", 10, 0, "name", "desc");

        assertBodyContains(bodyByAction.get("list-accounts"), "\"sortDirection\":\"desc\"");
        assertBodyContains(bodyByAction.get("list-namespaces"), "\"sortDirection\":\"desc\"");
        assertBodyContains(bodyByAction.get("list-user-groups"), "\"sortDirection\":\"desc\"");
    }

    // The overloads without a direction have to keep sending one rather than dropping the field.
    @Test
    void listRequestsDefaultToAscending() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"total\":0,\"accounts\":[],\"namespaces\":[],\"userGroups\":[]}");
        });

        EuclidSession session = newSession();
        session.listAccounts();
        session.listNamespaces("863459426936");
        session.listUserGroups();

        assertBodyContains(bodyByAction.get("list-accounts"), "\"sortDirection\":\"asc\"");
        assertBodyContains(bodyByAction.get("list-namespaces"), "\"sortDirection\":\"asc\"");
        assertBodyContains(bodyByAction.get("list-user-groups"), "\"sortDirection\":\"asc\"");
    }

    @Test
    void listAccessKeysParsesResponse() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{\"accessKeys\":[{\"accessKeyId\":\"AKIDEXAMPLE\",\"active\":true,\"createdAt\":\"2026-01-01\"}]}");
        });

        List<AccessKey> keys = newSession().listAccessKeys();

        assertEquals("list-access-keys", received.get().header("x-euclid-action"));
        assertEquals(1, keys.size());
        assertEquals("AKIDEXAMPLE", keys.get(0).accessKeyId());
        assertTrue(keys.getFirst().active());
    }

    @Test
    void deleteAccessKeySendsAccessKeyId() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newSession().deleteAccessKey("AKIDEXAMPLE");

        assertEquals("delete-access-key", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"accessKeyId\":\"AKIDEXAMPLE\"");
    }

    @Test
    void createUserGroupParsesResponse() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{\"userGroup\":" + userGroupJson("admins") + "}");
        });

        UserGroup group = newSession().createUserGroup("admins", "administrators");

        assertEquals("create-user-group", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"admins\"", "\"description\":\"administrators\"");
        assertEquals("admins", group.name());
        assertEquals(List.of("bob"), group.userIds());
    }

    @Test
    void listUserGroupsUsesDefaultsAndParsesResponse() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{\"userGroups\":[" + userGroupJson("admins") + "],\"total\":1}");
        });

        ListUserGroupsResponse response = newSession().listUserGroups();
        List<UserGroup> groups = response.userGroups();
        assertEquals(1, response.total());

        assertEquals("list-user-groups", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"prefix\":\"\"", "\"pageSize\":10", "\"pageIndex\":0",
                "\"sortColumn\":\"userId\"");
        assertEquals(1, groups.size());
        assertEquals("admins", groups.getFirst().name());
    }

    @Test
    void listUserGroupsWithExplicitParameters() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{\"userGroups\":[],\"total\":0}");
        });

        newSession().listUserGroups("adm", 25, 2, "name");

        assertBodyContains(received.get().body(), "\"prefix\":\"adm\"", "\"pageSize\":25", "\"pageIndex\":2",
                "\"sortColumn\":\"name\"");
    }

    @Test
    void addUserToUserGroupSendsErns() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newSession().addUserToUserGroup("group-ern", "user-ern");

        assertEquals("user-group-add-user", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"userGroup\":\"group-ern\"", "\"user\":\"user-ern\"");
    }

    @Test
    void removeUserFromUserGroupSendsErns() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newSession().removeUserFromUserGroup("group-ern", "user-ern");

        assertEquals("user-group-remove-user", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"userGroup\":\"group-ern\"", "\"user\":\"user-ern\"");
    }

    @Test
    void deleteUserGroupSendsName() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newSession().deleteUserGroup("admins");

        assertEquals("delete-user-group", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"admins\"");
    }

    @Test
    void createAccountParsesResponse() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{\"account\":" + accountJson("863459426936") + "}");
        });

        Account account = newSession().createAccount("863459426936", "Acme", "Acme's account");

        assertEquals("create-account", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"accountId\":\"863459426936\"", "\"name\":\"Acme\"",
                "\"description\":\"Acme's account\"");
        assertEquals("863459426936", account.accountId());
    }

    @Test
    void listAccountsUsesDefaultsAndParsesResponse() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{\"accounts\":[" + accountJson("863459426936") + "],\"total\":1}");
        });

        ListAccountsResponse response = newSession().listAccounts();
        List<Account> accounts = response.accounts();
        assertEquals(1, response.total());

        assertEquals("list-accounts", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"prefix\":\"\"", "\"pageSize\":10", "\"pageIndex\":0",
                "\"sortColumn\":\"accountId\"");
        assertEquals(1, accounts.size());
    }

    @Test
    void listAccountsWithExplicitParameters() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{\"accounts\":[],\"total\":0}");
        });

        newSession().listAccounts("86", 25, 2, "name");

        assertBodyContains(received.get().body(), "\"prefix\":\"86\"", "\"pageSize\":25", "\"pageIndex\":2",
                "\"sortColumn\":\"name\"");
    }

    @Test
    void deleteAccountSendsAccountId() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newSession().deleteAccount("863459426936");

        assertEquals("delete-account", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"accountId\":\"863459426936\"");
    }

    @Test
    void createNamespaceParsesResponse() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{\"namespace\":" + namespaceJson("prod") + "}");
        });

        Namespace namespace = newSession().createNamespace("863459426936", "prod", "production");

        assertEquals("create-namespace", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"accountId\":\"863459426936\"", "\"name\":\"prod\"",
                "\"description\":\"production\"");
        assertEquals("prod", namespace.name());
    }

    @Test
    void listNamespacesUsesDefaultsAndParsesResponse() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{\"namespaces\":[" + namespaceJson("prod") + "],\"total\":1}");
        });

        ListNamespacesResponse response = newSession().listNamespaces("863459426936");
        List<Namespace> namespaces = response.namespaces();
        assertEquals(1, response.total());

        assertEquals("list-namespaces", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"accountId\":\"863459426936\"", "\"prefix\":\"\"",
                "\"pageSize\":10", "\"pageIndex\":0", "\"sortColumn\":\"name\"");
        assertEquals(1, namespaces.size());
    }

    @Test
    void listNamespacesWithExplicitParameters() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{\"namespaces\":[],\"total\":0}");
        });

        newSession().listNamespaces("863459426936", "pr", 25, 2, "created");

        assertBodyContains(received.get().body(), "\"prefix\":\"pr\"", "\"pageSize\":25", "\"pageIndex\":2",
                "\"sortColumn\":\"created\"");
    }

    @Test
    void deleteNamespaceSendsAccountIdAndName() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newSession().deleteNamespace("863459426936", "prod");

        assertEquals("delete-namespace", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"accountId\":\"863459426936\"", "\"name\":\"prod\"");
    }

    @Test
    void grantNamespaceAccessSendsFields() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newSession().grantNamespaceAccess("user-ern", "863459426936", "prod");

        assertEquals("grant-namespace-access", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"user\":\"user-ern\"", "\"accountId\":\"863459426936\"",
                "\"namespace\":\"prod\"");
    }

    @Test
    void revokeNamespaceAccessSendsFields() throws Exception {
        AtomicReference<CapturedRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(capture(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newSession().revokeNamespaceAccess("user-ern", "863459426936", "prod");

        assertEquals("revoke-namespace-access", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"user\":\"user-ern\"", "\"accountId\":\"863459426936\"",
                "\"namespace\":\"prod\"");
    }

    @Test
    void nonSuccessResponseThrowsEuclidServiceException() throws Exception {
        server = startServer(exchange -> sendResponse(exchange, 500, "{\"error\":\"boom\"}"));

        EuclidSession session = newSession();
        EuclidServiceException exception =
                assertThrows(EuclidServiceException.class, () -> session.deleteUser("bob"));

        assertEquals("eam", exception.service());
        assertEquals("delete-user", exception.action());
        assertEquals(500, exception.statusCode());
        assertTrue(exception.responseBody().contains("boom"));
    }

    private EuclidSession newSession() {
        return new EuclidSession("test-token", "alice", "863459426936", "eu-central-1", null, null, false, "{}",
                baseUrl(), null, null);
    }

    private EuclidSession newSessionWithNamespace(String namespace) {
        return new EuclidSession("test-token", "alice", "863459426936", "eu-central-1", null, null, false, "{}",
                baseUrl(), null, namespace);
    }

    private static String userJson(String userId) {
        return "{\"userId\":\"" + userId + "\",\"ern\":\"user-ern\",\"password\":\"hash\",\"email\":\"bob@example.com\","
                + "\"accountId\":\"863459426936\",\"region\":\"eu-central-1\",\"accountGrants\":["
                + "{\"accountId\":\"863459426936\",\"namespaces\":[\"prod\"],\"isAdmin\":true,\"granted\":\"2026-01-01\"}],"
                + "\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}";
    }

    private static String userGroupJson(String name) {
        return "{\"name\":\"" + name + "\",\"ern\":\"group-ern\",\"accountId\":\"863459426936\","
                + "\"region\":\"eu-central-1\",\"description\":\"a group\",\"userIds\":[\"bob\"],"
                + "\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}";
    }

    private static String accountJson(String accountId) {
        return "{\"accountId\":\"" + accountId + "\",\"name\":\"Acme\",\"ern\":\"account-ern\","
                + "\"description\":\"Acme's account\",\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}";
    }

    private static String namespaceJson(String name) {
        return "{\"accountId\":\"863459426936\",\"name\":\"" + name + "\",\"ern\":\"namespace-ern\","
                + "\"description\":\"a namespace\",\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}";
    }

    private static void assertBodyContains(String body, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(body.contains(fragment), "expected body to contain " + fragment + " but was " + body);
        }
    }

    private static CapturedRequest capture(HttpExchange exchange) throws IOException {
        Headers requestHeaders = exchange.getRequestHeaders();
        CapturedRequest req = new CapturedRequest();
        for (String name : HEADERS_TO_CAPTURE) {
            String value = requestHeaders.getFirst(name);
            if (value != null) {
                req.headers.put(name.toLowerCase(), value);
            }
        }
        req.body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return req;
    }

    private HttpServer startServer(HttpHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/", handler);
        httpServer.start();
        return httpServer;
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static final class CapturedRequest {
        private final java.util.Map<String, String> headers = new java.util.HashMap<>();
        private String body;

        String header(String name) {
            return headers.get(name.toLowerCase());
        }

        String body() {
            return body;
        }
    }
}
