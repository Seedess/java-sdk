# Seedess Java SDK

Official Seedess SDK for Java.

**Server-side only.** The SDK uses a secret API key (`key_…`) to create and control streams.
Never ship it in a browser or a mobile app. Playback uses the separate `@seedess/*-p2p`
packages and a per-stream public token (`pk_…`).

## Install

```xml
<dependency>
  <groupId>com.seedess</groupId>
  <artifactId>sdk</artifactId>
  <version>0.1.0</version>
</dependency>
```

Java 17+.

## Use

```java
import com.seedess.SeedessClient;
import com.seedess.Models.CreateStreamInput;
import com.seedess.Models.Stream;

SeedessClient seedess = new SeedessClient(System.getenv("SEEDESS_API_KEY"));

Stream stream = seedess.createStream(
    new CreateStreamInput("https://origin.example/master.m3u8", "demo", null, true)
);

System.out.println(stream.publicToken());
System.out.println(stream.playback().master());
```

Pass only the public token and stream id to the page.

## Full working example

See [`examples/`](./examples) for a server that creates a stream and a browser page
that plays it with hls.js + the Seedess P2P adapter.

## Methods

| Method | HTTP | Purpose |
| --- | --- | --- |
| `createStream(CreateStreamInput)` | `POST /api/v1/streams` | Ingest a manifest, mint the public token. |
| `getStream(String)` | `GET /api/v1/streams/:id` | Status, variants, segment count. |
| `listStreams()` | `GET /api/v1/streams` | All streams in the tenant. |
| `refreshStream(String)` | `POST /api/v1/streams/:id/refresh` | Pull new live segments. |
| `disablePeers(String)` | `POST /api/v1/streams/:id/kill` | CDN only; playback continues. |
| `enablePeers(String)` | `POST /api/v1/streams/:id/revive` | Re-enable peer delivery. |
| `getMetrics(String)` | `GET /api/v1/streams/:id/metrics` | Offload and delivery counters. |
| `sendTelemetry(TelemetryInput)` | `POST /api/v1/telemetry` | One session batch (public, rate limited). |

`request(method, path, body, auth)` returns the raw Jackson `JsonNode`.

## Errors

```java
try {
    seedess.createStream(url);
} catch (SeedessException error) {
    System.out.println(error.status() + " " + error.errorCode() + " " + error.getMessage());
}
```

## Links

- Docs: https://seedess.com/docs/
- API: https://seedess.com/docs/api/
- Libraries: https://seedess.com/docs/libraries/

## License

MIT.
