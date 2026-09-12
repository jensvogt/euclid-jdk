# euclid-jdk

Java client library for the [Euclid](https://github.com/jensvogt/euclid) access and SQS APIs.

Requires Java 25.

## Installation

```xml
<dependency>
    <groupId>io.github.jensvogt</groupId>
    <artifactId>euclid-jdk</artifactId>
    <version>0.1.28</version>
</dependency>
```

## Usage

Log in once and reuse the cached session:

```java
EuclidSession session = Euclid.forServer("https://euclid.example.com")
        .access()
        .credentials("jens", "secret")
        .login();

EuclidSqs sqs = session.sqs();
```

The token returned by `login()` is cached in `~/.euclid/credentials` and reused
automatically while still valid, so subsequent calls can skip straight to:

```java
EuclidSqs sqs = Euclid.forServer("https://euclid.example.com").sqs();
```

### SQS operations

```java
String ern = sqs.createQueue("my-queue").ern();

sqs.sendMessage(ern, "{\"hello\":\"world\"}",
        Map.of("kind", new Variant("string", "greeting")));

ReceiveMessagesResponse response = sqs.receiveMessages(ern);
for (Message message : response.messages()) {
    System.out.println(message.body());
    sqs.deleteMessage(message.receiptHandle());
}

sqs.purgeQueue(ern);
sqs.deleteQueue(ern);
```

Other supported operations include `listQueues`, `getQueueErn`, `getMessageCount`,
`receiveAllMessages`, and `purgeAllQueues`. A queue can also be taken out of
service and put back with `stopQueue`/`startQueue`, have its default visibility
timeout changed with `setQueueVisibility`, and a dead letter queue drained back
onto the queues its messages came from with `redriveDlq`.

### ENS topics

ENS fans a published message out to everything subscribed to the topic, at publish
time. Where a queue message is consumed and gone, a topic is never consumed from -
what stays behind on it is a record of what was sent:

```java
EuclidEns ens = session.ens();

String ern = ens.createTopic("orders").ern();
ens.subscribe(ern, queueErn);

ens.publishMessage(ern, "{\"orderId\":\"4711\"}");
```

`createTopic(name, maxMessageLength)` caps how large a single message may be; the
default is 1 MiB. `deleteTopic(ern)` removes the topic, `purgeTopic(ern)` only what
is stored on it.

#### Publishing messages

A message can carry typed attributes, and a priority for the queue messages it fans
out into:

```java
ens.publishMessage(ern, "{\"orderId\":\"4711\"}",
        Map.of("kind", new Variant("string", "order")), "HIGH");
```

The priority belongs to those queue messages rather than to the topic message - a
topic is not consumed from, so a priority means nothing on it. It is carried so that
a delivery crossing a topic is still worth what it was sent as, instead of arriving
on the other side at `MIDDLE`.

`listMessages(topicErn)` reads what a topic has stored without consuming anything,
`getMessageCount(ern)` counts it, and a stored message's attributes can be read and
changed afterwards with `getMessageAttribute`/`setMessageAttribute`.

#### Subscriptions

`subscribe(topicErn, queueErn)` uses the `"SQS"` delivery protocol, which is the only
one there is for now, so the target is an EQS queue; the three-argument overload names
the protocol explicitly. `listSubscriptions(topicErn)` reports what is attached and
`unsubscribe(subscriptionErn)` detaches one.

#### Holding delivery

A topic can be told to hold what arrives instead of handing it over, which is what a
subscriber being redeployed or a downstream system taken down for the evening calls
for. A stopped topic still accepts and stores what is published to it - it simply
does not fan it out - and starting it again delivers the backlog, oldest first,
before the call returns:

```java
ens.stopTopic(ern);
// ... the subscriber is redeployed; publishes keep arriving and are held

TopicStatusResponse started = ens.startTopic(ern);
System.out.println(started.released() + " held messages delivered");
```

`getTopicMetadata(ern)` reports `status()` and, while stopped, `held()` - how much is
waiting. A start interrupted halfway through has delivered a prefix of the backlog
rather than none of it, so running it again picks up where it stopped.

#### Retention

How long a topic keeps what is published to it is `setTopicRetention(ern, seconds)`;
zero puts it back on the installation default and keeps it there as that changes,
rather than freezing a copy of today's value. `EuclidEns.RETENTION_FOREVER` (-1) keeps
everything - not a very long period but the absence of one, since the server stores
such a message with no expiry at all, which is what its TTL index ignores. That topic
then grows without limit and only `purgeTopic` empties it. Retention applies to
messages published afterwards - the ones already stored keep the expiry they were
given, because that is what the database's TTL index acts on.

Other supported operations include `listTopics`, `getTopicErn`, `purgeAllTopics`, and
topic tagging with `addTopicTag`/`setTopicTag`/`deleteTopicTag`.

### Events

Rather than polling for changes, an application can be called when they happen. A
listener registers its subscription, attaches the websocket to it, and hands each
event to a handler:

```java
EuclidEventStream stream = new EuclidEventStream(baseUrl, session.token(),
        session.getRegion(), session.getAccountId(), session.userId(),
        session.accessKeyId(), session.secretAccessKey(), null, "ees");

try (EuclidEventListener listener = EuclidEventListener.builder()
        .ees(session.ees())
        .stream(stream)
        .name("invoice-import")
        .eventTypes(List.of("esm.object.created"))
        .filter(Map.of("bucketName", "inbox"))
        .handler(event -> importInvoice(event.payload()))
        .build()) {

    listener.start();
    // events arrive on the handler until the listener is closed
}
```

The subscription is **durable** by default: matching events are stored under the
name and kept until they are acknowledged, so nothing is missed while the
application restarts, and events that arrived meanwhile are handled at startup.
An event is acknowledged only after the handler returns - if it throws, the event
is delivered again rather than lost. Two instances sharing a name share the work,
because each event is claimed by exactly one of them.

Pass `.mode(DeliveryMode.LIVE)` for a subscription that stores nothing and only
delivers while connected, which is what a view wants: a screen has no use for the
hour of events it missed while nobody was looking at it.

One stream can carry several listeners, and the same connection also still serves
`stream.awaitEvent(topic, filter, timeoutMillis)` for the simple "wait for the
next one" case.

### Secrets

ESS stores named secrets, encrypted under an EKM key - the namespace's own unless
one is named on creation:

```java
EuclidEss ess = session.ess();

ess.createSecret("db-password", "hunter2");

String password = ess.getSecret("db-password").value();

ess.rotateSecret("db-password", "hunter3");
```

`getSecret` is the only action that decrypts anything; `listSecrets`,
`addSecretTag` and the rest return metadata with no value attached. `updateSecret`
takes a request rather than positional arguments because it distinguishes an
absent field from an empty one: leaving the description null leaves the stored
description alone, while passing `""` clears it.

### The key/value store

EKV holds tables of items: a record identified by a **partition key**, optionally ordered
within that partition by a **sort key**. It is for the data that has no business being an object in
a bucket - a record read one at a time by name and updated in place, rather than a file written
once and listed by prefix:

```java
EuclidEkv ekv = session.ekv();

ekv.createTable("suppliers", "supplierId");

ekv.putItem("suppliers", Map.of("supplierId", "4711", "name", "Acme"));

Item supplier = ekv.getItem("suppliers", Map.of("supplierId", "4711"));
System.out.println(supplier.get("name"));
```

A sort key makes a partition readable as a range, which is the only reason to declare one:

```java
ekv.createTable(CreateTableRequest.builder().name("deliveries")
        .partitionKey("supplierId").sortKey("deliveredAt").build());

QueryResponse response = ekv.query("deliveries", "4711", SortOperator.BEGINS_WITH, "2026-09");
```

Items are ordinary JSON objects, carried here as `Map<String, Object>`, nested as deeply as you
like, with no type annotations to write and none to read back - and the types survive the round
trip: a number comes back a number, `3` does not become `3.0`, and an empty object stays an empty
object. Keys are typed when the table is created (`KeyType.STRING`, `NUMBER` or `BINARY`), which is
what makes a range query mean what it should: a number sort key orders 2, 9, 10, 100 rather than
"10" before "9".

Four things are worth knowing before you model against it:

- **`putItem` replaces**, it does not merge. Writing `{"supplierId":"4711","name":"x"}` over a
  fuller record leaves that record with two attributes. Read-modify-write until `updateItem` exists.
- **Attribute names** may not be empty, start with `$` or contain `.` - refused at the door rather
  than escaped, so what you read back is exactly what you wrote.
- **`_created` and `_modified`** are added to every item that is read, and lifted back out into
  `Item.created()` and `Item.modified()` so a read-modify-write does not store them. An attribute of
  the same name is shadowed.
- **A missing item is a 404**, and so an `EuclidServiceException` rather than a null: "there is no
  such item" and "here is an item with nothing in it" are different, and a caller should not have to
  tell them apart.

`listTables`, `describeTable`, `deleteTable`, `deleteItem` and `scan` round it out. Paging is by
page size and index, as everywhere else in euclid, rather than by cursor.

### The API gateway

EAG is a second, quite different listener from the one this library otherwise talks to. Where
euclid's own gateway speaks euclid's protocol - a target, an action and a JSON body - the API
gateway publishes paths to the outside world and proxies them to the applications EAP runs:

```java
EuclidEag eag = session.eag();

eag.createRoute("suppliers", "/suppliers", "supplier-api");

for (Route route : eag.listRoutes("/supp")) {
    System.out.println(route.path() + " -> " + route.applicationId());
}
```

A caller asks for `/suppliers/searchById?id=123` and has no idea which application answers it - that
is what a route says, and it is why the application's name never appears in the URL. Matching is by
longest path prefix on whole segments, so `/suppliers` carries everything beneath it but never
claims `/suppliers-intern`, and two routes may share a path only if their methods do not overlap.

A route can also reach one action of a euclid module instead of an application, which is the way in
for a front end that has to log in before it can call anything - otherwise it would talk to the API
gateway for the application and to euclid's own gateway for its credentials: two ports, two
origins, and CORS between them:

```java
eag.createModuleRoute("euclid-login", "/euclid/login", "eam", "login");
```

What a caller must present is decided per route, so one application can serve both a public read
and an operation only a euclid principal may perform:

```java
eag.createRoute(CreateRouteRequest.builder().routeId("suppliers-write").path("/suppliers")
        .applicationId("supplier-api").methods(List.of("POST", "PUT", "DELETE"))
        .authentication(RouteAuthentication.EUCLID).build());
```

`setRouteActive(routeId, false)` takes a route out of service without deleting it, so it comes back
exactly as it was; `updateRoute` changes only the fields it names, leaving the rest alone.
`listListeners()` reports which port speaks what, and for an HTTPS listener the certificate it
serves - including whether euclid minted it itself, which is what decides whether the port works
for a caller who has not been given it.

Every EAG action decides what is exposed to the outside world and on what terms, so all of them are
administrator-only: an ordinary principal gets HTTP 403.

### Tokens in a deployed application

An application euclid deploys is not handed a token. It is handed the name of a
file holding one, in `EUCLID_CREDENTIALS_FILE`, and euclid rewrites that file
with a fresh token once less than half the token's lifetime is left - roughly
every thirty minutes on the default one hour. A client that kept the token it
was built with would therefore work for about an hour and then start failing
with `401: Bearer token expired`, in the middle of whatever the application was
doing.

Clients read that file rather than remembering it, so this needs no code:

```java
// Inside an application euclid deployed - the token follows the file from here on.
EuclidEns ens = session.ens();
```

A client only does this when the file names the same user the client was built
for. An application that deliberately logs in as somebody else keeps the token
it logged in with, because swapping in the application's own identity would
change who the call is made as.

Anywhere else - a command-line tool, a job that runs and exits - the token a
client was given is the one it sends, as before. To read the managed
credentials from a client that would not pick them up on its own, or to renew a
token some other way, install a supplier (see `TokenRefreshable`):

```java
ens.token(CredentialsFileTokens.fromEnvironment());
ens.token(() -> myOwnRenewal.currentToken());
```

The supplier is asked once per request, so the reader stats the file and
re-reads it only when it has actually changed. If a token goes stale in flight
anyway, the client builds the credentials again and makes exactly one more
attempt - only on a 401 that says "expired", and only when the second attempt
would carry something different.

### Request signing

A client configured with an access key signs every request rather than sending a
bearer token. Two schemes are implemented: AWS SigV4, which is the default and
what euclid has always accepted, and [RFC 9421](https://www.rfc-editor.org/rfc/rfc9421.html)
HTTP Message Signatures, the standard scheme meant to replace it. Both use the
same access key and secret, so switching is a wire-format change and nothing else:

```java
EuclidEqs eqs = session.eqs();
eqs.signingScheme(SigningScheme.RFC9421);
```

The two do not collide - SigV4 signs into `Authorization`, RFC 9421 into
`Signature` and `Signature-Input` alongside an RFC 9530 `Content-Digest` - so a
server can accept both while a deployment moves one service at a time.
`SigningScheme.of(request)` reports which one a received request presents.

The signature covers the request line, the host, the body digest and euclid's
`x-euclid-*` routing headers. Which of the optional routing headers a request
carries is derived from the request itself on both sides, so adding or removing
one invalidates the signature rather than going unnoticed.

### TLS

When connecting to a server with a self-signed development certificate, point
at the CA cert (mirrors `euclid-cli --ca-cert`); this is picked up automatically
from `/etc/euclid/euclid_cert.crt` if present:

```java
Euclid.forServer(url).access().caCertPath("/path/to/ca.crt").credentials(user, pass).login();
```

## Building

```bash
mvn clean package
```

## Publishing

Releases are automated with
[release-please](https://github.com/googleapis/release-please): merging a
release PR to `master` publishes a signed artifact to Maven Central (see
`.github/workflows/maven.yml`).

## License

Apache License 2.0, see [LICENSE](LICENSE).
