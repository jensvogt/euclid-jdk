package de.jensvogt.euclid.module.emo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.jensvogt.euclid.auth.CredentialsFileTokens;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.auth.SigningScheme;
import de.jensvogt.euclid.auth.SigningSchemeSelectable;
import de.jensvogt.euclid.auth.TokenRefreshable;
import de.jensvogt.euclid.dto.emo.CreateAlertRuleRequest;
import de.jensvogt.euclid.dto.emo.ListAlertHistoryRequest;
import de.jensvogt.euclid.dto.emo.ListAlertsRequest;
import de.jensvogt.euclid.dto.emo.ListDashboardsResponse;
import de.jensvogt.euclid.dto.emo.Metric;
import de.jensvogt.euclid.dto.emo.MetricQueryRequest;
import de.jensvogt.euclid.dto.emo.SaveDashboardResponse;
import de.jensvogt.euclid.dto.emo.UpdateAlertRuleRequest;
import de.jensvogt.euclid.dto.emo.model.Alert;
import de.jensvogt.euclid.dto.emo.model.AlertRule;
import de.jensvogt.euclid.dto.emo.model.AlertTransition;
import de.jensvogt.euclid.dto.emo.model.Dashboard;
import de.jensvogt.euclid.dto.emo.model.DashboardSummary;
import de.jensvogt.euclid.dto.emo.model.MetricDataPoint;
import de.jensvogt.euclid.dto.emo.model.MetricInfo;
import de.jensvogt.euclid.exception.EuclidServiceException;
import de.jensvogt.euclid.http.EuclidHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * EMO (monitoring) operations for an authenticated
 * {@link de.jensvogt.euclid.module.eam.EuclidSession}.
 * <p>
 * Three parts. The first is pushing metrics an application has measured about itself. Everything
 * euclid knows about a process from the outside - is it running, is it answering - the manager
 * already sees; what it cannot see is how hard the process is working, and for an application that
 * consumes messages rather than serving requests that is the only signal there is. So the
 * application measures it and says so here.
 * <p>
 * Samples are accumulated by EMO into the same bucket-aligned rows its own collectors write, which
 * is what lets them be rolled up and queried alongside CPU, memory and the database gauges. Pushing
 * is therefore cheap and expected to be periodic: one call carrying every metric of one tick,
 * rather than a call per measurement.
 * <p>
 * The second is reading those rows back and alerting on them: listing what metrics there are,
 * reading a series' rows or averaging them, drawing threshold rules on them, and reading back where
 * every judged series stands and how it got there. These actions read the whole installation's
 * metrics and so are an administrator's: a caller without administrator privileges gets HTTP 403
 * from every one of them.
 * <p>
 * The third is the caller's own saved dashboard layouts. Those are not the installation's business
 * but one user's, so any authenticated caller may use them - and only on their own, since EMO files
 * every dashboard under the user it authenticated rather than under anything the request names.
 */
public final class EuclidEmo implements TokenRefreshable, SigningSchemeSelectable {

    /**
     * Jackson mapper used to serialise request bodies and parse responses. Nulls are left out, so a
     * request field the caller did not set is absent rather than null - which is what lets the
     * server apply its defaults on create and leave a field alone on update.
     */
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

    /**
     * Jackson mapper used only to write a dashboard layout. Unlike {@link #OBJECT_MAPPER} it keeps
     * nulls: a layout is the caller's own JSON, stored as given, and a null inside it is part of
     * what was given rather than an unset request field.
     */
    private static final ObjectMapper LAYOUT_MAPPER = new ObjectMapper();

    /**
     * The type a dashboard layout is read back as: a JSON object in its plain Java form.
     */
    private static final TypeReference<Map<String, Object>> LAYOUT_TYPE = new TypeReference<>() {
    };

    /**
     * The Euclid service every request from this class is addressed to, sent as the
     * {@code x-euclid-target} header.
     */
    private static final String TARGET = "emo";

    /**
     * The base URL of the Euclid server this instance talks to.
     */
    private final String baseUrl;

    /**
     * Supplies the bearer token for each request, used when no SigV4 access key is configured.
     *
     * <p>A supplier rather than a string so that a token which expires can be replaced without
     * rebuilding the client - see {@link TokenRefreshable#token(Supplier)}. A client built inside an
     * application euclid deployed follows the credentials file euclid rewrites; anywhere else it
     * holds a supplier returning the token it was given - see
     * {@link CredentialsFileTokens#forClient(String, String)}.
     */
    private volatile Supplier<String> token;

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
     * The namespace requests are scoped to, sent as the {@code x-euclid-namespace} header.
     */
    private final String nameSpace;

    /**
     * The scheme requests are signed with when an access key is configured.
     *
     * <p>Defaults to SigV4, which is what euclid has always accepted; a caller pointed at a server
     * that understands RFC 9421 switches it with {@link #signingScheme(SigningScheme)}. Volatile
     * because that call can come from a different thread than the requests it affects.
     */
    private volatile SigningScheme signingScheme = SigningScheme.SIGV4;

    /**
     * The HTTP client used for every request, pre-configured with this session's TLS trust.
     */
    private final EuclidHttpClient httpClient;

    /**
     * Constructs an EMO client. Normally obtained from
     * {@link de.jensvogt.euclid.module.eam.EuclidSession#emo()} rather than built directly.
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
    public EuclidEmo(String baseUrl, String token, String region, String accountId, String userId,
                     String accessKeyId, String secretAccessKey, String caCertPath, String nameSpace) {
        this.baseUrl = baseUrl;
        this.token = CredentialsFileTokens.forClient(token, userId);
        this.region = region;
        this.accountId = accountId;
        this.userId = userId;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.nameSpace = nameSpace;
        // The header factory is what lets a request whose token or signature expired in flight be
        // built again and sent once more - see EuclidHttpClient#headerFactory.
        this.httpClient = new EuclidHttpClient(caCertPath).headerFactory(this::requestHeaders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void token(Supplier<String> token) {
        this.token = Objects.requireNonNull(token, "token supplier must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void signingScheme(SigningScheme signingScheme) {
        this.signingScheme = Objects.requireNonNull(signingScheme, "signing scheme must not be null");
    }

    /**
     * Pushes one tick's worth of measurements.
     *
     * @param module the process reporting - recorded alongside the samples so EMO can tell which
     *               pusher a batch came from; an application's own id is the useful value here
     * @param metrics the samples, each a name, its dimensions, a value and a type
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public void pushMetrics(String module, List<Metric> metrics) throws IOException, InterruptedException {
        Objects.requireNonNull(module, "module must not be null");
        if (metrics == null || metrics.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("module", module);
        List<Map<String, Object>> items = new ArrayList<>(metrics.size());
        for (Metric metric : metrics) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", metric.name());
            // The dimensions as a map, which is what EMO stores. The older labelName/labelValue
            // pair is still accepted on the wire and still means the same thing, but sending both
            // would only repeat the first dimension.
            item.put("labels", metric.labels());
            item.put("value", metric.value());
            item.put("type", metric.type().wireName());
            items.add(item);
        }
        payload.put("items", items);

        post("push-metrics", OBJECT_MAPPER.writeValueAsString(payload));
    }

    /**
     * Lists every metric EMO has recorded in the last 24 hours, sorted by name.
     *
     * @return the metrics, each with its type, series count, label names and when it last reported
     * @throws EuclidServiceException with HTTP 401 if the caller is not authenticated, or 403 if it
     *                                is not an administrator
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     * @see #listMetrics(String, long)
     */
    public List<MetricInfo> listMetrics() throws IOException, InterruptedException {
        return listMetrics(null, 24);
    }

    /**
     * Lists the metrics EMO has recorded, by name - the question nothing else can answer without
     * already knowing the name.
     * <p>
     * Read over EMO's five-minute tier of the last {@code hours} hours, so a metric written hourly
     * (disk usage, say) is still in it with the default of 24, while one that stopped reporting a
     * week ago is not. The names are what {@link CreateAlertRuleRequest#name()} takes, and the label
     * names what a rule's label selector can match on.
     *
     * @param prefix only metrics whose name starts with this, e.g. {@code "system-"}; null or empty
     *               for every metric
     * @param hours  only metrics with data in this many hours; at least 1
     * @return the matching metrics, sorted by name
     * @throws EuclidServiceException with HTTP 400 if {@code hours} is below 1, 401 if the caller is
     *                                not authenticated, or 403 if it is not an administrator
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public List<MetricInfo> listMetrics(String prefix, long hours) throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hours", hours);
        if (prefix != null && !prefix.isEmpty()) {
            payload.put("prefix", prefix);
        }
        JsonNode root = post("list-metrics", OBJECT_MAPPER.writeValueAsString(payload));

        List<MetricInfo> metrics = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            metrics.add(new MetricInfo(
                    textOrNull(item, "name"),
                    textOrNull(item, "type"),
                    item.path("series").asLong(0),
                    toStringList(item.get("labelNames")),
                    textOrNull(item, "lastSeen")));
        }
        return metrics;
    }

    /**
     * Reads EMO's stored rows - the series themselves, as a graph draws them - newest first.
     * <p>
     * This is action {@code list}, the read side of everything EMO writes: its own collectors, every
     * module's pushed samples and {@link #pushMetrics(String, List)}. Each row is one bucket of one
     * series at the requested resolution; see {@link MetricDataPoint} for what a bucket holds. The
     * server caps the answer at 100 rows unless {@link MetricQueryRequest#limit()} says otherwise,
     * and since a busy installation writes many series per period, narrowing by name and time is
     * what makes that cap irrelevant rather than raising it.
     * <p>
     * Like {@link #listMetrics()}, this reads the whole installation's metrics and so is an
     * administrator's.
     *
     * @param request the filters; unset ones read the 100 newest {@code RAW} rows of every metric
     * @return the matching rows, newest first
     * @throws EuclidServiceException with HTTP 401 if the caller is not authenticated, or 403 if it
     *                                is not an administrator
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public List<MetricDataPoint> listMetricData(MetricQueryRequest request) throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request must not be null");
        JsonNode root = post("list", OBJECT_MAPPER.writeValueAsString(request));

        List<MetricDataPoint> points = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            points.add(new MetricDataPoint(
                    textOrNull(item, "name"),
                    toStringMap(item.get("labels")),
                    textOrNull(item, "labelName"),
                    textOrNull(item, "labelValue"),
                    item.path("value").asDouble(0),
                    item.path("minValue").asDouble(0),
                    item.path("maxValue").asDouble(0),
                    item.path("samples").asLong(0),
                    textOrNull(item, "type"),
                    textOrNull(item, "resolution"),
                    textOrNull(item, "timestamp")));
        }
        return points;
    }

    /**
     * Averages every stored row matching the request into one figure - the "what was it over the
     * last day" question, answered by the database rather than by fetching the rows.
     * <p>
     * This is action {@code average}. The mean is sample-weighted, as EMO's rollups are: buckets do
     * not all hold the same number of samples, and a plain mean of bucket means would let a quiet
     * bucket count as much as a busy one. Every matching row counts, whatever its dimensions, unless
     * the request narrows them - an average of a metric over all of its series is what an unnarrowed
     * request asks for. {@link MetricQueryRequest#limit()} is ignored.
     * <p>
     * No matching rows is {@code 0}, not an error, and so is an installation without MongoDB, which
     * cannot aggregate; a zero is therefore worth checking against {@link #listMetricData} before it
     * is believed. Administrator-only, like {@code list}.
     *
     * @param request the filters; usually at least a metric name and a time range
     * @return the sample-weighted mean of the matching rows, or {@code 0} if there are none
     * @throws EuclidServiceException with HTTP 401 if the caller is not authenticated, or 403 if it
     *                                is not an administrator
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public double averageMetric(MetricQueryRequest request) throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request must not be null");
        return post("average", OBJECT_MAPPER.writeValueAsString(request)).path("average").asDouble(0);
    }

    /**
     * Creates a threshold rule on one metric.
     * <p>
     * From the next period on, EMO judges every series the rule's labels match, and publishes each
     * change of state as {@code emo.alert.raised} or {@code emo.alert.cleared} on the event bus -
     * which is where something that wants to act on an alert subscribes, rather than polling
     * {@link #listAlerts(ListAlertsRequest)}.
     * <p>
     * The rule is checked whole on the server: a missing {@code ruleId} or metric, neither
     * threshold, thresholds the wrong way round for the comparison, or periods out of range are all
     * HTTP 400 with the reason. So is a rule matching more series than
     * {@code euclid.modules.emo.alert.max-series-per-rule} - one label selector too loose can
     * otherwise mean thousands of states written every period - which is narrowed with
     * {@code labels}.
     *
     * @param request the rule; unset fields take the server's defaults
     * @return the stored rule, with every default filled in
     * @throws EuclidServiceException with HTTP 400 if the rule is invalid or matches too many series,
     *                                401 if the caller is not authenticated, 403 if it is not an
     *                                administrator, 409 if a rule with that id already exists in the
     *                                account, or 500 if the rule could not be stored
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public AlertRule createAlertRule(CreateAlertRuleRequest request) throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request must not be null");
        return toAlertRule(post("create-alert-rule", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Changes an existing threshold rule. Only the fields set on the request are sent, and the
     * server only touches the fields it receives.
     * <p>
     * A changed threshold applies from the next period, to the series as they stand - that is the
     * change somebody moving a line wants to see. A changed metric or label selector, or disabling
     * the rule, instead ends the series it was judging: each one not OK is cleared and published as
     * {@code emo.alert.cleared} with reason {@code rule-changed} or {@code rule-disabled}, so nobody
     * waits on an alert the rule can no longer clear. The rule as it would stand after the change is
     * validated as a whole, exactly as on create.
     *
     * @param request the change to apply; only {@code ruleId} is required
     * @return the stored rule after the change
     * @throws EuclidServiceException with HTTP 400 if {@code ruleId} is missing or the changed rule is
     *                                invalid, 401 if the caller is not authenticated, 403 if it is
     *                                not an administrator, 404 if no such rule exists in the account,
     *                                or 500 if the rule could not be stored
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public AlertRule updateAlertRule(UpdateAlertRuleRequest request) throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request must not be null");
        // The removals are the only nulls the server should see: NON_NULL drops every unset field,
        // and an explicit null is how the server is told to take a threshold away.
        ObjectNode body = OBJECT_MAPPER.valueToTree(request);
        if (request.removeWarning()) {
            body.putNull("warning");
        }
        if (request.removeCritical()) {
            body.putNull("critical");
        }
        return toAlertRule(post("update-alert-rule", OBJECT_MAPPER.writeValueAsString(body)));
    }

    /**
     * Deletes a threshold rule and the state of every series it judged.
     * <p>
     * Each series not OK is cleared first and published as {@code emo.alert.cleared} with reason
     * {@code rule-deleted}, so a subscriber told CRITICAL is also told it is over. The rule's history
     * is kept until it expires.
     *
     * @param ruleId the rule to delete
     * @throws EuclidServiceException with HTTP 400 if {@code ruleId} is empty, 401 if the caller is
     *                                not authenticated, 403 if it is not an administrator, or 404 if
     *                                no such rule exists in the account
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public void deleteAlertRule(String ruleId) throws IOException, InterruptedException {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        post("delete-alert-rule", OBJECT_MAPPER.writeValueAsString(Map.of("ruleId", ruleId)));
    }

    /**
     * Lists every threshold rule in the account.
     *
     * @return the rules
     * @throws EuclidServiceException with HTTP 401 if the caller is not authenticated, or 403 if it
     *                                is not an administrator
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public List<AlertRule> listAlertRules() throws IOException, InterruptedException {
        return listAlertRules(null);
    }

    /**
     * Lists the account's threshold rules, or only the one named.
     * <p>
     * A rule that does not exist is an empty list rather than HTTP 404, since this is a filter on a
     * listing rather than a lookup.
     *
     * @param ruleId only this rule; null or empty for every rule
     * @return the matching rules
     * @throws EuclidServiceException with HTTP 401 if the caller is not authenticated, or 403 if it
     *                                is not an administrator
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public List<AlertRule> listAlertRules(String ruleId) throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (ruleId != null && !ruleId.isEmpty()) {
            payload.put("ruleId", ruleId);
        }
        JsonNode root = post("list-alert-rules", OBJECT_MAPPER.writeValueAsString(payload));

        List<AlertRule> rules = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            rules.add(toAlertRule(item));
        }
        return rules;
    }

    /**
     * Shows where every series a rule judges stands now: its state, since when, the last figure
     * judged and the lines it was judged against.
     * <p>
     * A series a rule has not seen a row of yet is not listed. This is the current picture - what a
     * dashboard colours a panel from; for what changed and when, see
     * {@link #listAlertHistory(ListAlertHistoryRequest)}.
     *
     * @param request the filters; unset ones list every series of every rule
     * @return the matching series
     * @throws EuclidServiceException with HTTP 401 if the caller is not authenticated, or 403 if it
     *                                is not an administrator
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public List<Alert> listAlerts(ListAlertsRequest request) throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request must not be null");
        JsonNode root = post("list-alerts", OBJECT_MAPPER.writeValueAsString(request));

        List<Alert> alerts = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            alerts.add(new Alert(
                    textOrNull(item, "ruleId"),
                    textOrNull(item, "name"),
                    toStringMap(item.get("labels")),
                    textOrNull(item, "state"),
                    textOrNull(item, "severity"),
                    textOrNull(item, "since"),
                    item.path("value").asDouble(0),
                    textOrNull(item, "lastSeen"),
                    textOrNull(item, "statistic"),
                    textOrNull(item, "comparison"),
                    doubleOrNull(item, "warning"),
                    doubleOrNull(item, "critical"),
                    textOrNull(item, "description"),
                    textOrNull(item, "namespace")));
        }
        return alerts;
    }

    /**
     * Shows the recorded alert state changes, newest first: which series, from what to what, the
     * figure and the line that decided it.
     * <p>
     * Changes are kept for {@code euclid.modules.emo.alert.history-retention} days, 90 as shipped,
     * and outlive the rule that made them.
     *
     * @param request the filters; unset ones return the 100 most recent changes of every rule
     * @return the matching changes, newest first
     * @throws EuclidServiceException with HTTP 401 if the caller is not authenticated, or 403 if it
     *                                is not an administrator
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public List<AlertTransition> listAlertHistory(ListAlertHistoryRequest request)
            throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request must not be null");
        JsonNode root = post("list-alert-history", OBJECT_MAPPER.writeValueAsString(request));

        List<AlertTransition> transitions = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            transitions.add(new AlertTransition(
                    textOrNull(item, "ruleId"),
                    textOrNull(item, "name"),
                    toStringMap(item.get("labels")),
                    textOrNull(item, "previous"),
                    textOrNull(item, "state"),
                    textOrNull(item, "severity"),
                    item.path("value").asDouble(0),
                    doubleOrNull(item, "threshold"),
                    textOrNull(item, "statistic"),
                    textOrNull(item, "reason"),
                    textOrNull(item, "timestamp"),
                    textOrNull(item, "accountId"),
                    textOrNull(item, "namespace")));
        }
        return transitions;
    }

    /**
     * Saves one of the caller's dashboard layouts under a name, replacing any already saved under
     * it.
     * <p>
     * EMO stores the layout as the JSON object it is given and does not look inside, so its shape is
     * the caller's own. The owner is never part of the request: EMO files the dashboard under the
     * user it authenticated, which is the whole of the access control on dashboards - there is no
     * way to name another user, so no way to overwrite theirs - and why, unlike everything else in
     * this class but {@link #pushMetrics}, these actions need no administrator. Re-saving keeps the
     * original creation time.
     *
     * @param name      the dashboard's name, unique among the caller's own dashboards
     * @param dashboard the layout; serialised with Jackson, so any value Jackson writes as a JSON
     *                  object - nested maps, lists, numbers, strings - will do
     * @return the name, the user it was saved for and when
     * @throws EuclidServiceException with HTTP 400 if {@code name} is empty, or 401 if the caller is
     *                                not authenticated
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public SaveDashboardResponse saveDashboard(String name, Map<String, ?> dashboard)
            throws IOException, InterruptedException {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(dashboard, "dashboard must not be null");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("dashboard", dashboard);
        // Written with a mapper that keeps nulls: NON_NULL would quietly drop a null the layout
        // itself holds, and what is stored should be what was given.
        JsonNode root = post("save-dashboard", LAYOUT_MAPPER.writeValueAsString(payload));
        return new SaveDashboardResponse(
                textOrNull(root, "name"),
                textOrNull(root, "userId"),
                textOrNull(root, "modified"));
    }

    /**
     * Reads one of the caller's saved dashboards, layout included, exactly as it was saved.
     *
     * @param name the dashboard's name
     * @return the dashboard, its layout and when it was created and last saved
     * @throws EuclidServiceException with HTTP 400 if {@code name} is empty, 401 if the caller is not
     *                                authenticated, 404 if the caller has no dashboard of that name,
     *                                or 500 if the stored layout is no longer readable JSON
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public Dashboard getDashboard(String name) throws IOException, InterruptedException {
        Objects.requireNonNull(name, "name must not be null");
        JsonNode root = post("get-dashboard", OBJECT_MAPPER.writeValueAsString(Map.of("name", name)));
        JsonNode layout = root.get("dashboard");
        Map<String, Object> dashboard = layout != null && layout.isObject()
                ? OBJECT_MAPPER.convertValue(layout, LAYOUT_TYPE)
                : null;
        return new Dashboard(
                textOrNull(root, "name"),
                dashboard,
                textOrNull(root, "created"),
                textOrNull(root, "modified"));
    }

    /**
     * Lists the caller's saved dashboards by name, without their layouts - what a chooser needs,
     * and nothing a chooser does not.
     *
     * @return whose dashboards they are, which is always the caller, and the dashboards
     * @throws EuclidServiceException with HTTP 401 if the caller is not authenticated
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public ListDashboardsResponse listDashboards() throws IOException, InterruptedException {
        JsonNode root = post("list-dashboards", "{}");

        List<DashboardSummary> dashboards = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            dashboards.add(new DashboardSummary(
                    textOrNull(item, "name"),
                    textOrNull(item, "created"),
                    textOrNull(item, "modified")));
        }
        return new ListDashboardsResponse(textOrNull(root, "userId"), dashboards);
    }

    /**
     * Deletes one of the caller's saved dashboards. Only the layout goes; the metrics it showed are
     * not touched.
     *
     * @param name the dashboard's name
     * @throws EuclidServiceException with HTTP 400 if {@code name} is empty, 401 if the caller is not
     *                                authenticated, or 404 if the caller has no dashboard of that
     *                                name
     * @throws IOException if the request could not be sent
     * @throws InterruptedException if the calling thread was interrupted while waiting
     */
    public void deleteDashboard(String name) throws IOException, InterruptedException {
        Objects.requireNonNull(name, "name must not be null");
        post("delete-dashboard", OBJECT_MAPPER.writeValueAsString(Map.of("name", name)));
    }

    /**
     * Builds an {@link AlertRule} from the rule JSON create-alert-rule and update-alert-rule answer
     * with, and list-alert-rules lists.
     *
     * @param node the JSON object describing the rule
     * @return the parsed rule
     */
    private static AlertRule toAlertRule(JsonNode node) {
        return new AlertRule(
                textOrNull(node, "ruleId"),
                textOrNull(node, "name"),
                toStringMap(node.get("labels")),
                textOrNull(node, "resolution"),
                textOrNull(node, "statistic"),
                textOrNull(node, "comparison"),
                doubleOrNull(node, "warning"),
                doubleOrNull(node, "critical"),
                node.path("forPeriods").asLong(0),
                node.path("clearPeriods").asLong(0),
                node.path("noDataAfterPeriods").asLong(0),
                node.path("enabled").asBoolean(false),
                textOrNull(node, "description"),
                textOrNull(node, "accountId"),
                textOrNull(node, "namespace"),
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
     * Converts a JsonNode holding an object of strings - a series' labels - into a map.
     *
     * @param node the JsonNode to convert
     * @return the map, in the server's order, or an empty map if the node is null or not an object
     */
    private static Map<String, String> toStringMap(JsonNode node) {
        Map<String, String> values = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.properties().forEach(entry -> values.put(entry.getKey(), entry.getValue().asText()));
        }
        return values;
    }

    /**
     * Reads a text field, telling a field that is absent or null apart from an empty one.
     *
     * @param node  the JSON object to read from
     * @param field the field name
     * @return the field's text, or {@code null} if it is absent or null
     */
    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * Reads a number that may be absent or null - a threshold the rule does not have - without
     * turning that into zero, which would be a line drawn at zero.
     *
     * @param node  the JSON object to read from
     * @param field the field name
     * @return the field's value, or {@code null} if it is absent, null or not a number
     */
    private static Double doubleOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.isNumber() ? null : value.asDouble();
    }

    /**
     * Posts one of EMO's actions and parses the response body, since every one of them takes a JSON
     * request and answers with JSON.
     *
     * @param action the EMO action to post
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
     * Generates a map of HTTP request headers for a specified action and request body.
     * The headers include content type, region, account ID, user ID, and
     * authentication information. If AWS credentials are available, the headers
     * are signed with the client's configured {@link SigningScheme} - SigV4 unless
     * {@link #signingScheme(SigningScheme)} says otherwise; without an access key, a Bearer token is
     * used and nothing is signed.
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
            signSignatureHeaders(signable, TARGET, headers);
        } else {
            headers.put("Authorization", "Bearer " + token.get());
        }
        return headers;
    }

    /**
     * Signs {@code signable} with the configured scheme and copies the headers it produced onto the
     * outgoing request.
     * <p>
     * Which headers those are is the scheme's business rather than this method's: SigV4 signs into
     * Authorization alongside two {@code x-amz-*} headers, RFC 9421 into Signature and
     * Signature-Input alongside Content-Digest. The scheme is read once into a local so that a
     * {@link #signingScheme(SigningScheme)} call arriving mid-request cannot sign with one scheme
     * and then copy the header names of the other.
     *
     * @param signable the request to sign, with every header the signature covers and the body
     *                 already set on it
     * @param service  the service to scope the signature to
     * @param headers  the outgoing headers, which the signature headers are added to in place
     */
    private void signSignatureHeaders(SignableRequest signable, String service, Map<String, String> headers) {
        signable.scheme(URI.create(baseUrl).getScheme());
        SigningScheme scheme = signingScheme;
        scheme.sign(signable, accessKeyId, secretAccessKey, region, service);
        for (String header : scheme.signatureHeaderNames()) {
            headers.put(header, signable.header(header));
        }
    }

    /**
     * Builds the {@code host} header value the request signature is computed over, including the port
     * when the base URL names one.
     *
     * @return the host header value
     */
    private String hostHeader() {
        URI uri = URI.create(baseUrl);
        int port = uri.getPort();
        return port == -1 ? uri.getHost() : uri.getHost() + ":" + port;
    }
}
