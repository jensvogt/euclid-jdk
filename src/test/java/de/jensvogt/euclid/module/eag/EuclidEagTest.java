package de.jensvogt.euclid.module.eag;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.eag.CreateRouteRequest;
import de.jensvogt.euclid.dto.eag.ListListenersResponse;
import de.jensvogt.euclid.dto.eag.UpdateRouteRequest;
import de.jensvogt.euclid.dto.eag.model.Listener;
import de.jensvogt.euclid.dto.eag.model.Protocol;
import de.jensvogt.euclid.dto.eag.model.Route;
import de.jensvogt.euclid.dto.eag.model.RouteAuthentication;
import de.jensvogt.euclid.exception.EuclidServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEag authenticates the way it claims to (SigV4-signed when an access key is
 * configured, bearer token otherwise, mirroring euclid-cli's HttpClient.cpp), routes every route
 * and listener action to the right request with a correctly-shaped body, parses the corresponding
 * response, and surfaces non-2xx responses as {@link EuclidServiceException}.
 * <p>
 * The update-route cases carry most of the weight here: EAG reads a field's presence rather than
 * its value, so a client that serialized its nulls would silently republish a path somebody meant
 * to leave alone - or, worse, reset an authentication requirement to nothing.
 */
class EuclidEagTest {

    private static final List<String> SIGNED_HEADERS = List.of("host", "x-amz-content-sha256", "x-amz-date",
            "x-euclid-account-id", "x-euclid-action", "x-euclid-region", "x-euclid-target", "x-euclid-user-id",
            "x-euclid-namespace");

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void createRouteSignsWithSigV4WhenAccessKeyConfigured() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, routeJson());
        });

        new EuclidEag(baseUrl(), "unused-token", "eu-central-1", "863459426936", "alice",
                accessKeyId, secretAccessKey, null, null)
                .createRoute("suppliers", "/suppliers", "supplier-api");

        SignableRequest req = received.get();
        assertTrue(req.header("authorization").startsWith("AWS4-HMAC-SHA256 "));

        Optional<SigV4.VerifyResult> result = SigV4.verify(req,
                id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
        assertTrue(result.isPresent(), "server-side verification of the client's own signature must succeed");
        assertEquals(accessKeyId, result.get().accessKeyId());
        assertEquals("eag", req.header("x-euclid-target"));
    }

    @Test
    void createRouteUsesTheBearerTokenWithoutAnAccessKey() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, routeJson());
        });

        newClient().createRoute("suppliers", "/suppliers", "supplier-api");

        assertEquals("Bearer test-token", received.get().header("authorization"));
    }

    @Test
    void createRouteSendsThePathAndApplicationAndParsesTheRoute() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, routeJson());
        });

        Route route = newClient().createRoute("suppliers", "/suppliers", "supplier-api");

        assertEquals("create-route", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"routeId\":\"suppliers\"", "\"path\":\"/suppliers\"",
                "\"applicationId\":\"supplier-api\"");
        assertEquals("suppliers", route.routeId());
        assertEquals("ern:eag:route/suppliers", route.ern());
        assertEquals("/suppliers", route.path());
        assertEquals("supplier-api", route.applicationId());
        assertEquals(List.of("GET", "POST"), route.methods());
        assertEquals(RouteAuthentication.EUCLID, route.authentication());
        assertTrue(route.active());
        assertFalse(route.isModuleRoute());
    }

    // The scope a route acts in is defaulted by the server when it is not named, so an unset region
    // or namespace has to stay off the wire rather than arriving as an empty string.
    @Test
    void createRouteOmitsTheFieldsItWasNotGiven() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, routeJson());
        });

        newClient().createRoute("suppliers", "/suppliers", "supplier-api");

        String body = received.get().body();
        assertFalse(body.contains("region"), "an unset region must not reach the server at all: " + body);
        assertFalse(body.contains("namespace"), "an unset namespace must not reach the server at all: " + body);
        assertFalse(body.contains("moduleTarget"), "an application route must not name a module: " + body);
        assertFalse(body.contains("active"), "an unset active flag must not reach the server at all: " + body);
    }

    @Test
    void createRouteCanSpellOutMethodsScopeAndAuthentication() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, routeJson());
        });

        newClient().createRoute(CreateRouteRequest.builder().routeId("suppliers").path("/suppliers")
                .applicationId("supplier-api").methods(List.of("GET", "HEAD")).region("eu-west-1")
                .namespace("development").authentication(RouteAuthentication.BASIC).active(false).build());

        assertBodyContains(received.get().body(), "\"methods\":[\"GET\",\"HEAD\"]", "\"region\":\"eu-west-1\"",
                "\"namespace\":\"development\"", "\"authentication\":\"BASIC\"", "\"active\":false");
    }

    @Test
    void createModuleRouteNamesTheModuleAndItsOneAction() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, moduleRouteJson());
        });

        Route route = newClient().createModuleRoute("euclid-login", "/euclid/login", "eam", "login");

        String body = received.get().body();
        assertBodyContains(body, "\"moduleTarget\":\"eam\"", "\"moduleAction\":\"login\"",
                "\"path\":\"/euclid/login\"");
        assertFalse(body.contains("applicationId"), "a module route must not name an application: " + body);
        assertTrue(route.isModuleRoute());
        assertEquals("eam", route.moduleTarget());
        assertEquals("login", route.moduleAction());
        assertTrue(route.methods().isEmpty(), "a route naming no methods answers for all of them");
    }

    @Test
    void createRouteSurfacesAPathThatIsAlreadyRouted() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 409, "{\"error\":\"Path and method are already routed by routeId: other\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().createRoute("suppliers", "/suppliers", "supplier-api"));

        assertEquals("eag", exception.service());
        assertEquals("create-route", exception.action());
        assertEquals(409, exception.statusCode());
    }

    // A route to an application that was never deployed would answer 503 forever, which looks like
    // an application that is down rather than one that does not exist.
    @Test
    void createRouteSurfacesAnApplicationThatDoesNotExist() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 404, "{\"error\":\"Application not found, applicationId: nope\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().createRoute("suppliers", "/suppliers", "nope"));

        assertEquals(404, exception.statusCode());
    }

    // Every EAG action decides what the outside world can reach, so an ordinary principal is turned
    // away by the server.
    @Test
    void anOrdinaryPrincipalIsRefused() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 403, "{\"error\":\"Administrator privileges required\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().listRoutes());

        assertEquals(403, exception.statusCode());
    }

    // The heart of it: EAG asks whether a field is present, not what it holds. An omitted path has
    // to stay omitted on the wire, or the server reads it as a path to republish under.
    @Test
    void updateRouteOmitsTheFieldsItWasNotGiven() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, routeJson());
        });

        newClient().updateRoute(UpdateRouteRequest.builder().routeId("suppliers")
                .authentication(RouteAuthentication.EUCLID).build());

        String body = received.get().body();
        assertEquals("update-route", received.get().header("x-euclid-action"));
        assertBodyContains(body, "\"routeId\":\"suppliers\"", "\"authentication\":\"EUCLID\"");
        assertFalse(body.contains("path"), "an unset path must not reach the server at all: " + body);
        assertFalse(body.contains("methods"), "an unset method list must not reach the server at all: " + body);
        assertFalse(body.contains("applicationId"), "an unset application must not reach the server at all: " + body);
        assertFalse(body.contains("active"), "an unset active flag must not reach the server at all: " + body);
    }

    // The other half of the same rule: an explicitly empty method list is a request to answer for
    // every method, and has to survive serialization rather than being dropped.
    @Test
    void updateRouteSendsAnEmptyMethodListWhenAskedToAnswerForAll() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, routeJson());
        });

        newClient().updateRoute(UpdateRouteRequest.builder().routeId("suppliers").methods(List.of()).build());

        assertBodyContains(received.get().body(), "\"methods\":[]");
    }

    @Test
    void setRouteActiveTakesARouteOutOfServiceWithoutTouchingAnythingElse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, routeJson().replace("\"active\":true", "\"active\":false"));
        });

        Route route = newClient().setRouteActive("suppliers", false);

        String body = received.get().body();
        assertEquals("update-route", received.get().header("x-euclid-action"));
        assertBodyContains(body, "\"routeId\":\"suppliers\"", "\"active\":false");
        assertFalse(body.contains("path"), "deactivating must not restate the path: " + body);
        assertFalse(route.active());
    }

    @Test
    void updateRouteSurfacesAnUnknownAuthenticationKind() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 400, "{\"error\":\"authentication must be \\\"NONE\\\" or \\\"EUCLID\\\"\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().updateRoute(UpdateRouteRequest.builder().routeId("suppliers")
                        .authentication(RouteAuthentication.UNKNOWN).build()));

        assertEquals(400, exception.statusCode());
    }

    @Test
    void listRoutesFiltersByPathPrefix() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"routes\":[" + routeJson() + "," + moduleRouteJson() + "]}");
        });

        List<Route> routes = newClient().listRoutes("/supp");

        assertEquals("list-routes", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"prefix\":\"/supp\"");
        assertEquals(2, routes.size());
        assertEquals("suppliers", routes.getFirst().routeId());
        assertTrue(routes.get(1).isModuleRoute());
    }

    @Test
    void listRoutesToleratesAnEmptyResult() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{}");
        });

        assertTrue(newClient().listRoutes().isEmpty());
    }

    @Test
    void getRouteSendsTheRouteId() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, routeJson());
        });

        Route route = newClient().getRoute("suppliers");

        assertEquals("get-route", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"routeId\":\"suppliers\"");
        assertEquals("/suppliers", route.path());
    }

    @Test
    void getRouteSurfacesARouteThatDoesNotExist() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 404, "{\"error\":\"Route not found, routeId: nope\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().getRoute("nope"));

        assertEquals("get-route", exception.action());
        assertEquals(404, exception.statusCode());
    }

    // delete-route answers 200 with nothing at all, which the client has to accept rather than
    // choke on while trying to parse it.
    @Test
    void deleteRouteAcceptsAnEmptyBody() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "");
        });

        newClient().deleteRoute("suppliers");

        assertEquals("delete-route", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"routeId\":\"suppliers\"");
    }

    // An unrecognized kind must not read back as NONE: reporting a route as public when the server
    // may think otherwise is the one mistake here that would not announce itself.
    @Test
    void anUnrecognizedAuthenticationKindReadsBackAsUnknown() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, routeJson().replace("\"EUCLID\"", "\"MTLS\""));
        });

        assertEquals(RouteAuthentication.UNKNOWN, newClient().getRoute("suppliers").authentication());
    }

    @Test
    void listListenersGathersTheCertificateFields() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"listeners\":[" + httpsListenerJson() + "],\"total\":1,\"serving\":true}");
        });

        ListListenersResponse response = newClient().listListeners();

        assertEquals("list-listeners", received.get().header("x-euclid-action"));
        assertEquals(1, response.total());
        assertTrue(response.serving());

        Listener listener = response.listeners().getFirst();
        assertEquals("development", listener.namespace());
        assertEquals(8443, listener.port());
        assertEquals(Protocol.HTTPS, listener.protocol());
        assertTrue(listener.serving());
        assertEquals("eag-development", listener.certificateName());
        assertFalse(listener.namesCertificate(), "a listener taking the conventional certificate names none");

        assertNotNull(listener.certificate());
        assertEquals("ern:ekm:certificate/eag-development", listener.certificate().ern());
        assertEquals("CN=euclid", listener.certificate().subject());
        assertEquals(List.of("localhost", "euclid.example.com"), listener.certificate().subjectAltNames());
        assertTrue(listener.certificate().generated());
        assertFalse(listener.certificate().expired());
    }

    // A plain HTTP port has no certificate to be missing, and reporting one as absent would read as
    // a fault rather than a setting.
    @Test
    void aPlainHttpListenerHasNoCertificate() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"listeners\":[{\"namespace\":\"\",\"port\":8080,\"protocol\":\"http\","
                    + "\"serving\":true,\"certificate\":\"\",\"certificateConfigured\":\"\",\"certificateFound\":false}],"
                    + "\"total\":1,\"serving\":true}");
        });

        Listener listener = newClient().listListeners().listeners().getFirst();

        assertEquals(Protocol.HTTP, listener.protocol());
        assertNull(listener.certificate());
        assertFalse(listener.namesCertificate());
    }

    // A configured port whose bind failed is still listed - it is the one somebody is looking for -
    // but nothing it says is being served.
    @Test
    void aListenerThatCouldNotBindIsStillListed() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"listeners\":[{\"namespace\":\"integration\",\"port\":8081,"
                    + "\"protocol\":\"https\",\"serving\":false,\"certificate\":\"own-cert\","
                    + "\"certificateConfigured\":\"own-cert\",\"certificateFound\":false}],"
                    + "\"total\":1,\"serving\":false}");
        });

        ListListenersResponse response = newClient().listListeners();

        assertFalse(response.serving());
        assertEquals(1, response.listeners().size());
        assertTrue(response.listeners().getFirst().namesCertificate());
        assertNull(response.listeners().getFirst().certificate(), "a certificate that was not found is not one");
    }

    @Test
    void listListenersToleratesAnEmptyResult() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"listeners\":[],\"total\":0,\"serving\":false}");
        });

        assertTrue(newClient().listListeners().listeners().isEmpty());
    }

    @Test
    void namespaceIsSentWhenTheSessionIsScoped() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        new EuclidEag(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, "prod")
                .listRoutes();

        assertEquals("prod", received.get().header("x-euclid-namespace"));
    }

    @Test
    void namespaceHeaderIsOmittedWhenUnset() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().listRoutes();

        assertEquals("", received.get().header("x-euclid-namespace"));
    }

    @Test
    void aRouteWithoutOptionalFieldsIsStillReadable() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"routeId\":\"suppliers\",\"path\":\"/suppliers\"}");
        });

        Route route = newClient().getRoute("suppliers");

        assertEquals("suppliers", route.routeId());
        assertNull(route.applicationId());
        assertTrue(route.methods().isEmpty());
        assertTrue(route.active(), "a route says nothing about being inactive unless it is");
        assertEquals(RouteAuthentication.UNKNOWN, route.authentication());
    }

    private static String routeJson() {
        return "{\"routeId\":\"suppliers\",\"ern\":\"ern:eag:route/suppliers\",\"accountId\":\"863459426936\","
                + "\"region\":\"eu-central-1\",\"namespace\":\"development\",\"path\":\"/suppliers\","
                + "\"applicationId\":\"supplier-api\",\"moduleTarget\":\"\",\"moduleAction\":\"\","
                + "\"methods\":[\"GET\",\"POST\"],\"authentication\":\"EUCLID\",\"active\":true,"
                + "\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}";
    }

    private static String moduleRouteJson() {
        return "{\"routeId\":\"euclid-login\",\"ern\":\"ern:eag:route/euclid-login\","
                + "\"accountId\":\"863459426936\",\"region\":\"eu-central-1\",\"namespace\":\"\","
                + "\"path\":\"/euclid/login\",\"applicationId\":\"\",\"moduleTarget\":\"eam\","
                + "\"moduleAction\":\"login\",\"methods\":[],\"authentication\":\"NONE\",\"active\":true,"
                + "\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}";
    }

    private static String httpsListenerJson() {
        return "{\"namespace\":\"development\",\"port\":8443,\"protocol\":\"https\",\"serving\":true,"
                + "\"certificate\":\"eag-development\",\"certificateConfigured\":\"\",\"certificateFound\":true,"
                + "\"certificateErn\":\"ern:ekm:certificate/eag-development\",\"certificateSubject\":\"CN=euclid\","
                + "\"certificateIssuer\":\"CN=euclid\",\"certificateSerialNumber\":\"01\","
                + "\"certificateFingerprint\":\"ab:cd\","
                + "\"certificateSubjectAltNames\":[\"localhost\",\"euclid.example.com\"],"
                + "\"certificateGenerated\":true,\"certificateNotBefore\":\"2026-01-01T00:00:00Z\","
                + "\"certificateNotAfter\":\"2027-01-01T00:00:00Z\",\"certificateExpired\":false}";
    }

    private EuclidEag newClient() {
        return new EuclidEag(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, null);
    }

    private static void assertBodyContains(String body, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(body.contains(fragment), "expected body to contain " + fragment + " but was " + body);
        }
    }

    private static SignableRequest captureRequest(HttpExchange exchange) throws IOException {
        SignableRequest req = new SignableRequest(exchange.getRequestMethod(), exchange.getRequestURI().toString());
        Headers requestHeaders = exchange.getRequestHeaders();
        for (String name : SIGNED_HEADERS) {
            String value = requestHeaders.getFirst(name);
            if (value != null) {
                req.header(name, value);
            }
        }
        String authorization = requestHeaders.getFirst("Authorization");
        if (authorization != null) {
            req.header("authorization", authorization);
        }
        req.body(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
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
}
