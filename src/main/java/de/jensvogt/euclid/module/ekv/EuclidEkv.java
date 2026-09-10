package de.jensvogt.euclid.module.ekv;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.auth.CredentialsFileTokens;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.auth.SigningScheme;
import de.jensvogt.euclid.auth.SigningSchemeSelectable;
import de.jensvogt.euclid.auth.TokenRefreshable;
import de.jensvogt.euclid.dto.ekv.CreateTableRequest;
import de.jensvogt.euclid.dto.ekv.ItemKeyRequest;
import de.jensvogt.euclid.dto.ekv.ListTablesRequest;
import de.jensvogt.euclid.dto.ekv.ListTablesResponse;
import de.jensvogt.euclid.dto.ekv.PutItemRequest;
import de.jensvogt.euclid.dto.ekv.QueryRequest;
import de.jensvogt.euclid.dto.ekv.QueryResponse;
import de.jensvogt.euclid.dto.ekv.ScanRequest;
import de.jensvogt.euclid.dto.ekv.ScanResponse;
import de.jensvogt.euclid.dto.ekv.TableNameRequest;
import de.jensvogt.euclid.dto.ekv.model.Item;
import de.jensvogt.euclid.dto.ekv.model.KeyType;
import de.jensvogt.euclid.dto.ekv.model.SortOperator;
import de.jensvogt.euclid.dto.ekv.model.TableDescription;
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
 * EKV (key/value store) operations for an authenticated
 * {@link de.jensvogt.euclid.module.eam.EuclidSession}: tables of items, looked up by key and
 * queried by sort-key range.
 * <p>
 * This is for the data that has no business being an object in a bucket - a record read one at a
 * time by name and updated in place, rather than a file written once and listed by prefix. A table
 * declares a <em>partition key</em>, which identifies an item, and optionally a <em>sort key</em>,
 * which orders the items sharing a partition key and is what makes {@link #query} a range read
 * rather than a lookup. Nothing else about an item is declared: two items in the same table need
 * have no attribute in common beyond the key.
 * <p>
 * Items are ordinary JSON objects with no type annotations to write and none to read back, carried
 * here as {@code Map<String, Object>} - and the types survive the round trip: a number comes back a
 * number, {@code 3} does not become {@code 3.0}, and an empty object stays an empty object.
 * <p>
 * Four things are worth knowing before modelling against it:
 * <ul>
 *   <li>{@link #putItem} <strong>replaces</strong>, it does not merge. Writing
 *       {@code {"supplierId":"4711","name":"x"}} over a fuller record leaves that record with two
 *       attributes. Read, change and write back until the server grows an update-item action.</li>
 *   <li><strong>Attribute names</strong> may not be empty, start with {@code $} or contain
 *       {@code .} - refused at the door rather than escaped, so what comes back is exactly what
 *       went in.</li>
 *   <li><strong>{@code _created} and {@code _modified}</strong> are added to every item that is
 *       read, and lifted back out into {@link Item#created()} and {@link Item#modified()} here. An
 *       attribute of the same name is shadowed.</li>
 *   <li><strong>Paging is by page size and index</strong>, as everywhere else in euclid, rather
 *       than by cursor.</li>
 * </ul>
 * <p>
 * Not there yet on the server, in the order they are likely to arrive: update-item and conditional
 * writes, batch reads and writes, secondary indexes, a TTL attribute, and item changes published on
 * the event bus.
 */
public final class EuclidEkv implements TokenRefreshable, SigningSchemeSelectable {

    /**
     * A singleton instance of {@code ObjectMapper} from the Jackson library used for
     * serializing Java objects to JSON and deserializing JSON to Java objects.
     * <p>
     * Left at its default null handling, unlike ESS's: EKV keeps an attribute that is present and
     * empty distinct from one that is absent, so a null inside an item is a value to be written
     * rather than a field to be dropped.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * The type an item's attributes are read back as - whatever JSON types they arrived as, rather
     * than a shape this client fixes.
     */
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    /**
     * The Euclid service every request from this class is addressed to, sent as the
     * {@code x-euclid-target} header.
     */
    private static final String TARGET = "ekv";

    /**
     * The attribute the server reports an item's creation timestamp under, and which is lifted out
     * of the attributes into {@link Item#created()}.
     */
    private static final String CREATED_ATTRIBUTE = "_created";

    /**
     * The attribute the server reports an item's modification timestamp under, and which is lifted
     * out of the attributes into {@link Item#modified()}.
     */
    private static final String MODIFIED_ATTRIBUTE = "_modified";

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
     * The namespace requests are scoped to, sent as the {@code x-euclid-namespace} header. A table
     * is created into this namespace, so this is what a table's ERN is built from.
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
     * Constructs an EKV client. Normally obtained from
     * {@link de.jensvogt.euclid.module.eam.EuclidSession#ekv()} rather than built directly.
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
    public EuclidEkv(String baseUrl, String token, String region, String accountId, String userId,
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
     * Creates a table keyed on a single string attribute, holding records looked up one at a time
     * by name.
     *
     * @param name         the table name, unique within the account
     * @param partitionKey the attribute every item is identified by
     * @return the table as it was created, with an item count of zero
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public TableDescription createTable(String name, String partitionKey) throws IOException, InterruptedException {
        return createTable(CreateTableRequest.builder().name(name).partitionKey(partitionKey).build());
    }

    /**
     * Creates a table whose items are ordered within their partition by a second string attribute,
     * which is what makes the partition readable as a range with {@link #query}.
     *
     * @param name         the table name, unique within the account
     * @param partitionKey the attribute every item is identified by
     * @param sortKey      the attribute items sharing a partition key are ordered by, which has to
     *                     be a different attribute from {@code partitionKey}
     * @return the table as it was created, with an item count of zero
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public TableDescription createTable(String name, String partitionKey, String sortKey)
            throws IOException, InterruptedException {
        return createTable(CreateTableRequest.builder().name(name).partitionKey(partitionKey).sortKey(sortKey).build());
    }

    /**
     * Creates a table, naming the types of its key attributes.
     * <p>
     * The types are what makes a range query mean what it should: a {@link KeyType#NUMBER} sort key
     * orders 2, 9, 10, 100 rather than putting "10" before "9". They cannot be changed afterwards,
     * and every item written has to honour them.
     * <p>
     * Refused with HTTP 409 if a table of that name already exists.
     *
     * @param request the table to create
     * @return the table as it was created, with an item count of zero
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public TableDescription createTable(CreateTableRequest request) throws IOException, InterruptedException {
        return toTableDescription(post("create-table", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Describes a table: its key, and how many items it holds.
     * <p>
     * The item count is counted rather than looked up, so this is not free on a large table.
     *
     * @param name the table to describe
     * @return the table's description
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public TableDescription describeTable(String name) throws IOException, InterruptedException {
        return toTableDescription(post("describe-table", tableNameBody(name)));
    }

    /**
     * Lists the tables of this session's account and namespace, using default paging.
     *
     * @return a {@code ListTablesResponse} carrying the tables and their total
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListTablesResponse listTables() throws IOException, InterruptedException {
        return listTables("", 10, 0, "name", "asc");
    }

    /**
     * Lists tables, optionally filtered by name prefix and paginated. Each one is described as
     * {@link #describeTable} would describe it, item count included - which is counted per table,
     * so a large page of large tables costs what those counts cost.
     *
     * @param prefix        only tables whose name starts with this prefix are returned
     * @param pageSize      the maximum number of tables to return in a single page
     * @param pageIndex     the zero-based index of the page to return
     * @param sortColumn    the column results are sorted by
     * @param sortDirection the direction to sort in, {@code "asc"} or {@code "desc"}
     * @return a {@code ListTablesResponse} carrying the tables and their total
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListTablesResponse listTables(String prefix, long pageSize, long pageIndex, String sortColumn,
                                         String sortDirection) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListTablesRequest.builder().prefix(prefix).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).sortDirection(sortDirection).build());
        JsonNode root = post("list-tables", body);
        return ListTablesResponse.builder().tables(toTableDescriptionList(root.get("tables")))
                .total(root.path("total").asLong(0)).build();
    }

    /**
     * Deletes a table and every item in it. There is no confirmation and nothing is kept.
     *
     * @param name the table to delete
     * @return how many items went with it
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public long deleteTable(String name) throws IOException, InterruptedException {
        return post("delete-table", tableNameBody(name)).path("deletedItems").asLong(0);
    }

    /**
     * Writes an item, replacing whatever was stored under its key.
     * <p>
     * It replaces rather than merges: an item written with two attributes has two attributes
     * afterwards, whatever it had before. The item has to carry the table's key attributes with the
     * types the table declared for them, and no attribute name may be empty, start with {@code $}
     * or contain {@code .}.
     *
     * @param table the table to write to
     * @param item  the item, whose values may be scalars, lists or nested maps
     * @return the item as it was stored, with its timestamps
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Item putItem(String table, Map<String, Object> item) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(PutItemRequest.builder().table(table).item(item).build());
        return toItem(post("put-item", body));
    }

    /**
     * Reads one item by its key.
     * <p>
     * The key names the table's key attributes and only those - the partition key alone where the
     * table has no sort key, both where it has one.
     * <p>
     * An item that is not there is HTTP 404, and so an {@link EuclidServiceException} rather than a
     * null: "there is no such item" and "here is an item with nothing in it" are different, and a
     * caller should not have to tell them apart. Catch it and check
     * {@link EuclidServiceException#statusCode()} where a miss is an ordinary outcome.
     *
     * @param table the table to read from
     * @param key   the key attributes and their values
     * @return the stored item
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Item getItem(String table, Map<String, Object> key) throws IOException, InterruptedException {
        return toItem(post("get-item", itemKeyBody(table, key)));
    }

    /**
     * Removes one item by its key.
     *
     * @param table the table to remove from
     * @param key   the key attributes and their values
     * @return whether there was an item to remove
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public boolean deleteItem(String table, Map<String, Object> key) throws IOException, InterruptedException {
        return post("delete-item", itemKeyBody(table, key)).path("deleted").asBoolean(false);
    }

    /**
     * Reads a whole partition, in ascending sort-key order.
     *
     * @param table        the table to query
     * @param partitionKey the partition key's value, of the type the table declared for it
     * @return the partition's items, ordered by sort key
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public QueryResponse query(String table, Object partitionKey) throws IOException, InterruptedException {
        return query(QueryRequest.builder().table(table).partitionKey(partitionKey).build());
    }

    /**
     * Reads the items of one partition whose sort key satisfies a condition, in ascending sort-key
     * order.
     * <p>
     * Narrowing at all needs a table that declares a sort key; asking one that does not for
     * anything but {@link SortOperator#NONE} is refused with HTTP 400. Use
     * {@link #query(QueryRequest)} for {@link SortOperator#BETWEEN}, which needs an upper bound as
     * well.
     *
     * @param table        the table to query
     * @param partitionKey the partition key's value, of the type the table declared for it
     * @param sortOperator how to narrow by sort key
     * @param sortValue    what to compare the sort key against, of the type the table declared for it
     * @return the matching items, ordered by sort key
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public QueryResponse query(String table, Object partitionKey, SortOperator sortOperator, Object sortValue)
            throws IOException, InterruptedException {
        return query(QueryRequest.builder().table(table).partitionKey(partitionKey)
                .sortOperator(sortOperator).sortValue(sortValue).build());
    }

    /**
     * Reads the items of one partition, with the ordering, the sort-key condition and the paging
     * spelled out.
     *
     * @param request what to read
     * @return the matching items, in the order asked for
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public QueryResponse query(QueryRequest request) throws IOException, InterruptedException {
        JsonNode root = post("query", OBJECT_MAPPER.writeValueAsString(request));
        List<Item> items = toItemList(root.get("items"));
        return QueryResponse.builder().items(items).count(root.path("count").asLong(items.size())).build();
    }

    /**
     * Reads a table's items without regard to their key, using no paging - so the whole table
     * comes back.
     *
     * @param table the table to scan
     * @return the table's items and how many it holds
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ScanResponse scan(String table) throws IOException, InterruptedException {
        return scan(table, 0, 0);
    }

    /**
     * Reads a page of a table's items without regard to their key.
     * <p>
     * This reads the table rather than an index: fine for a small table or an export, the wrong
     * tool for a lookup. {@link ScanResponse#total()} says how much there is to get through.
     *
     * @param table     the table to scan
     * @param pageSize  the maximum number of items to return; 0 means no limit
     * @param pageIndex the zero-based index of the page to return
     * @return the items on that page and how many the table holds
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ScanResponse scan(String table, long pageSize, long pageIndex) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ScanRequest.builder().table(table).pageSize(pageSize).pageIndex(pageIndex).build());
        JsonNode root = post("scan", body);
        List<Item> items = toItemList(root.get("items"));
        return ScanResponse.builder().items(items).count(root.path("count").asLong(items.size()))
                .total(root.path("total").asLong(0)).build();
    }

    /**
     * Builds the request body for the two actions that name nothing but a table.
     *
     * @param name the table the action applies to
     * @return the serialized request body
     * @throws IOException if the request cannot be serialized
     */
    private static String tableNameBody(String name) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(TableNameRequest.builder().name(name).build());
    }

    /**
     * Builds the request body for the two actions that name nothing but a key.
     *
     * @param table the table the action applies to
     * @param key   the key attributes and their values
     * @return the serialized request body
     * @throws IOException if the request cannot be serialized
     */
    private static String itemKeyBody(String table, Map<String, Object> key) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(ItemKeyRequest.builder().table(table).key(key).build());
    }

    /**
     * Posts one of EKV's actions and parses the response body, since every one of them takes a JSON
     * request and answers with JSON.
     *
     * @param action the EKV action to post
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
     * Converts a JsonNode holding an array of table descriptions into a list of them.
     *
     * @param tablesNode the JsonNode representing the array of tables
     * @return the parsed descriptions, or an empty list if the node is null or not an array
     */
    private static List<TableDescription> toTableDescriptionList(JsonNode tablesNode) {
        List<TableDescription> tables = new ArrayList<>();
        if (tablesNode != null && tablesNode.isArray()) {
            for (JsonNode tableNode : tablesNode) {
                tables.add(toTableDescription(tableNode));
            }
        }
        return tables;
    }

    /**
     * Builds a {@link TableDescription} from the table JSON the table actions answer with.
     *
     * @param node the JSON object describing the table
     * @return the parsed description, or {@code null} if the node is null
     */
    private static TableDescription toTableDescription(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new TableDescription(
                textOrNull(node, "name"),
                textOrNull(node, "ern"),
                textOrNull(node, "partitionKey"),
                KeyType.fromWireValue(textOrNull(node, "partitionKeyType")),
                emptyToNull(textOrNull(node, "sortKey")),
                KeyType.fromWireValue(textOrNull(node, "sortKeyType")),
                node.path("itemCount").asLong(0),
                textOrNull(node, "created"),
                textOrNull(node, "modified"));
    }

    /**
     * Converts a JsonNode holding an array of items into a list of them.
     *
     * @param itemsNode the JsonNode representing the array of items
     * @return the parsed items, or an empty list if the node is null or not an array
     */
    private static List<Item> toItemList(JsonNode itemsNode) {
        List<Item> items = new ArrayList<>();
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode itemNode : itemsNode) {
                items.add(toItem(itemNode));
            }
        }
        return items;
    }

    /**
     * Builds an {@link Item} from the item JSON every item action answers with, lifting the two
     * timestamps out of the attributes.
     * <p>
     * They come back as ordinary attributes named {@code _created} and {@code _modified}, and
     * leaving them there would mean an item read, changed and written back acquires two attributes
     * it never had - {@link #putItem} replaces rather than merges, so that would stick.
     *
     * @param node the JSON object describing the item
     * @return the parsed item, or {@code null} if the node is null or not an object
     */
    private static Item toItem(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        Map<String, Object> attributes = OBJECT_MAPPER.convertValue(node, OBJECT_MAP);
        Object created = attributes.remove(CREATED_ATTRIBUTE);
        Object modified = attributes.remove(MODIFIED_ATTRIBUTE);
        return new Item(attributes, created == null ? null : created.toString(),
                modified == null ? null : modified.toString());
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

    /**
     * Reads a string field from a JSON object, mapping both an absent field and a JSON null to
     * {@code null}.
     *
     * @param node the JSON object to read from
     * @param field the field name
     * @return the field's value, or {@code null} if it is absent or null
     */
    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * Maps an empty string to {@code null}, for the sort key a table without one reports as "".
     *
     * @param value the value to map
     * @return the value, or {@code null} if it was null or empty
     */
    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
