# euclid-jdk

Java client library for the [Euclid](https://github.com/jensvogt/euclid) access and SQS APIs.

Requires Java 25.

## Installation

```xml
<dependency>
    <groupId>io.github.jensvogt</groupId>
    <artifactId>euclid-jdk</artifactId>
    <version>0.1.0</version>
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
`receiveAllMessages`, and `purgeAllQueues`.

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
