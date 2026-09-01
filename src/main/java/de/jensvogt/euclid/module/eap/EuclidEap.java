package de.jensvogt.euclid.module.eap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.eap.ApplicationRequest;
import de.jensvogt.euclid.dto.eap.CreateApplicationRequest;
import de.jensvogt.euclid.dto.eap.ListApplicationsRequest;
import de.jensvogt.euclid.dto.eap.UpdateApplicationRequest;
import de.jensvogt.euclid.dto.eap.model.Application;
import de.jensvogt.euclid.exception.EuclidServiceException;
import de.jensvogt.euclid.http.EuclidHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EAP (application platform) operations for an authenticated
 * {@link de.jensvogt.euclid.module.eam.EuclidSession}. Mirrors euclid-cli's {@code EapCli}.
 * <p>
 * An application is an artifact stored in an ESM bucket that euclid runs as a pool of processes.
 * Every action here decides which code euclid executes and under whose identity it does so - about
 * as privileged as this system gets - so the server requires administrator rights for all of them
 * and answers HTTP 403 otherwise.
 * <p>
 * Deploying resolves names eagerly: the bucket, the artifact within it, and every bucket or queue
 * named as a resource grant must already exist, so a typo is a rejected deployment rather than an
 * application that starts and is then denied everything.
 * <p>
 * Starting and stopping only record intent. {@link #startApplication} and {@link #stopApplication}
 * set {@code desiredState}; euclid-mgr's reconciler is what launches or tears down the processes,
 * so {@link Application#state()} can lag {@link Application#desiredState()} briefly after either.
 */
public final class EuclidEap {

    /**
     * A singleton instance of {@code ObjectMapper} from the Jackson library used for
     * serializing Java objects to JSON and deserializing JSON to Java objects.
     * <p>
     * Configured to leave null fields out of the serialized request entirely rather than writing
     * them as {@code null}. EAP distinguishes an absent field from a present one:
     * update-application only touches the fields it receives, and sending {@code "arguments": null}
     * would be read as an empty list and silently clear the application's argument list rather than
     * leave it alone.
     */
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * The Euclid service every request from this class is addressed to, sent as the
     * {@code x-euclid-target} header.
     */
    private static final String TARGET = "eap";

    /**
     * The base URL of the Euclid server this instance talks to.
     */
    private final String baseUrl;

    /**
     * The bearer token issued at login, used when no SigV4 access key is configured.
     */
    private final String token;

    /**
     * The region requests are made in.
     */
    private final String region;

    /**
     * The account requests are made on behalf of.
     */
    private final String accountId;

    /**
     * The user requests are made on behalf of.
     */
    private final String userId;

    /**
     * Public identifier of the SigV4 access key, or {@code null} to authenticate with the token.
     */
    private final String accessKeyId;

    /**
     * Secret paired with {@link #accessKeyId}, or {@code null} to authenticate with the token.
     */
    private final String secretAccessKey;

    /**
     * The namespace requests are scoped to, sent as the {@code x-euclid-namespace} header. A
     * technical principal EAP mints for an application is created in this namespace.
     */
    private final String nameSpace;

    /**
     * The HTTP client used for every request, pre-configured with this session's TLS trust.
     */
    private final EuclidHttpClient httpClient;

    /**
     * Constructs an EAP client. Normally obtained from
     * {@link de.jensvogt.euclid.module.eam.EuclidSession#eap()} rather than built directly.
     *
     * @param baseUrl         the base URL of the Euclid server
     * @param token           the bearer token issued at login
     * @param region          the region requests are made in
     * @param accountId       the account requests are made on behalf of
     * @param userId          the user requests are made on behalf of
     * @param accessKeyId     public identifier of the SigV4 access key, or {@code null} for token auth
     * @param secretAccessKey secret paired with {@code accessKeyId}, or {@code null} for token auth
     * @param caCertPath      path to an additional PEM CA certificate to trust, or {@code null}
     * @param nameSpace       the namespace requests are scoped to, or {@code null} if unscoped
     */
    public EuclidEap(String baseUrl, String token, String region, String accountId, String userId,
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
     * Deploys a new application. It is created stopped - call {@link #startApplication} to run it.
     * <p>
     * Everything named is resolved now rather than at start-up, so a bucket, artifact, granted
     * resource or user that does not exist is reported here as HTTP 404. An application ID already
     * in use is refused with HTTP 409.
     * <p>
     * Leaving {@code user} unset is the usual choice: EAP mints a technical principal for the
     * application, grants it exactly the named resources, and removes it again when the application
     * is deleted - which is what keeps an application from borrowing a person's credentials. Naming
     * an existing user instead requires that user to already have an access key to sign with.
     *
     * @param request the application definition; unset fields take the server's defaults
     * @return the stored definition, with names resolved to ERNs
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Application createApplication(CreateApplicationRequest request)
            throws IOException, InterruptedException {
        return toApplication(post("create-application", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Changes a deployed application. Only the fields set on the request are sent, and the server
     * only touches the fields it receives, so one setting can be changed without resending the
     * whole definition.
     * <p>
     * A field that is set replaces the current value outright - passing {@code arguments}
     * substitutes the whole list rather than appending to it. Changing {@code buckets} or
     * {@code queues} also rewrites the grants on the principal the application runs as, provided
     * that principal is one EAP minted itself.
     *
     * @param request the change to apply; only {@code applicationId} is required
     * @return the stored definition after the change
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Application updateApplication(UpdateApplicationRequest request)
            throws IOException, InterruptedException {
        return toApplication(post("update-application", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Lists every deployed application.
     *
     * @return the deployed applications
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public List<Application> listApplications() throws IOException, InterruptedException {
        return listApplications("");
    }

    /**
     * Lists the applications whose ID starts with the given prefix.
     *
     * @param prefix only applications whose ID starts with this prefix are returned; empty lists
     *               them all
     * @return the matching applications
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public List<Application> listApplications(String prefix) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(ListApplicationsRequest.builder().prefix(prefix).build());
        JsonNode root = post("list-applications", body);

        List<Application> applications = new ArrayList<>();
        JsonNode applicationsNode = root.get("applications");
        if (applicationsNode != null && applicationsNode.isArray()) {
            for (JsonNode applicationNode : applicationsNode) {
                applications.add(toApplication(applicationNode));
            }
        }
        return applications;
    }

    /**
     * Retrieves a single application by ID.
     *
     * @param applicationId the ID of the application
     * @return the application's definition
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Application getApplication(String applicationId) throws IOException, InterruptedException {
        return toApplication(post("get-application", applicationBody(applicationId)));
    }

    /**
     * Deletes an application's definition, which is also what stops it: the reconciler runs whatever
     * is defined and RUNNING, so a definition that no longer exists is torn down on its next tick.
     * <p>
     * A technical principal EAP minted for this application goes with it - a credential outliving
     * the thing it was issued to is exactly the orphan that arrangement exists to avoid. A user
     * named at deployment is left alone.
     *
     * @param applicationId the ID of the application to delete
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void deleteApplication(String applicationId) throws IOException, InterruptedException {
        post("delete-application", applicationBody(applicationId));
    }

    /**
     * Records that an application should be running. euclid-mgr's reconciler is what starts the
     * processes, so the returned {@link Application#state()} may still read {@code "STOPPED"} with
     * {@link Application#instances()} at zero even though {@link Application#desiredState()} is now
     * {@code "RUNNING"}.
     *
     * @param applicationId the ID of the application to start
     * @return the stored definition, with its new desired state
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Application startApplication(String applicationId) throws IOException, InterruptedException {
        return toApplication(post("start-application", applicationBody(applicationId)));
    }

    /**
     * Records that an application should be stopped, leaving its definition in place. The reconciler
     * tears the processes down; see {@link #startApplication} on the lag between desired and
     * observed state.
     *
     * @param applicationId the ID of the application to stop
     * @return the stored definition, with its new desired state
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Application stopApplication(String applicationId) throws IOException, InterruptedException {
        return toApplication(post("stop-application", applicationBody(applicationId)));
    }

    /**
     * Builds the request body for the four actions that name nothing but an application.
     *
     * @param applicationId the ID of the application the action applies to
     * @return the serialized request body
     * @throws IOException if the request cannot be serialized
     */
    private static String applicationBody(String applicationId) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(ApplicationRequest.builder().applicationId(applicationId).build());
    }

    /**
     * Posts one of EAP's actions and parses the response body, since every one of them takes a JSON
     * request and answers with JSON.
     *
     * @param action the EAP action to post
     * @param body the JSON request body
     * @return the parsed response body
     * @throws IOException if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    private JsonNode post(String action, String body) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, TARGET, action,
                requestHeaders(action, body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException(TARGET, action, response.statusCode(), response.body());
        }

        return OBJECT_MAPPER.readTree(response.body());
    }

    /**
     * Builds an {@link Application} from the definition JSON every EAP action but list-applications
     * and delete-application answers with.
     *
     * @param node the JSON object describing the application
     * @return the parsed application
     */
    private static Application toApplication(JsonNode node) {
        return new Application(
                textOrNull(node, "applicationId"),
                textOrNull(node, "ern"),
                textOrNull(node, "accountId"),
                textOrNull(node, "region"),
                textOrNull(node, "runtime"),
                textOrNull(node, "bucketErn"),
                textOrNull(node, "artifactKey"),
                textOrNull(node, "command"),
                toStringList(node.get("arguments")),
                toStringMap(node.get("environment")),
                toStringList(node.get("resources")),
                textOrNull(node, "userId"),
                node.path("minInstances").asLong(0),
                node.path("maxInstances").asLong(0),
                node.path("readyTimeoutMs").asLong(0),
                textOrNull(node, "desiredState"),
                textOrNull(node, "state"),
                node.path("instances").asLong(0),
                textOrNull(node, "created"),
                textOrNull(node, "modified"));
    }

    /**
     * Converts a JsonNode holding an array of strings into a list.
     *
     * @param node the JsonNode to convert
     * @return the list of strings, or an empty list if the node is null or not an array
     */
    private static List<String> toStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode element : node) {
                values.add(element.asText());
            }
        }
        return values;
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
     * Generates a map of HTTP request headers for a specified action and request body.
     * The headers include content type, region, account ID, user ID, and
     * authentication information. If AWS credentials are available, the headers
     * are signed using the SigV4 signing process; otherwise, a Bearer token is used.
     *
     * @param action the action being performed by the request.
     * @param body the body of the request to be included for signing.
     * @return a map of HTTP headers constructed for the request.
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
     * Builds the {@code host} header value the SigV4 signature is computed over, including the port
     * when the base URL names one.
     *
     * @return the host header value
     */
    private String hostHeader() {
        URI uri = URI.create(baseUrl);
        int port = uri.getPort();
        return port == -1 ? uri.getHost() : uri.getHost() + ":" + port;
    }

    /**
     * Reads a text field from a JSON node, tolerating both an absent field and an explicit null.
     *
     * @param node the node to read from
     * @param field the field name
     * @return the field's text value, or {@code null} if it is absent or null
     */
    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
