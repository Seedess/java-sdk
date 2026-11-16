package com.seedess;

import com.sun.net.httpserver.HttpServer;
import com.seedess.Models.CreateStreamInput;
import com.seedess.Models.TelemetryInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SeedessClientTest {
    private HttpServer server;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> auth = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private String baseUrl;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            auth.set(exchange.getRequestHeaders().getFirst("authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String response = exchange.getRequestURI().getPath().endsWith("/metrics")
                ? "{\"ok\":true,\"id\":\"demo\",\"metrics\":{\"cdnBytes\":10,\"p2pBytes\":90,\"offloadPercent\":90}}"
                : "{\"ok\":true,\"id\":\"demo\",\"publicToken\":\"pk_1\",\"streams\":[{\"id\":\"demo\"}]}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("content-type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void createStreamSendsAuthAndBody() {
        SeedessClient client = new SeedessClient("key_test", baseUrl);
        Models.Stream stream = client.createStream(new CreateStreamInput("https://origin/master.m3u8", "demo", null, true));

        assertEquals("demo", stream.id());
        assertEquals("/api/v1/streams", path.get());
        assertEquals("Bearer key_test", auth.get());
        assertTrue(body.get().contains("\"id\":\"demo\""));
    }

    @Test
    void disablePeersMapsToKill() {
        SeedessClient client = new SeedessClient("key_test", baseUrl);
        client.disablePeers("demo");
        assertEquals("/api/v1/streams/demo/kill", path.get());
    }

    @Test
    void getMetricsReturnsNumbers() {
        SeedessClient client = new SeedessClient("key_test", baseUrl);
        Models.Metrics metrics = client.getMetrics("demo");
        assertEquals(90, metrics.p2pBytes());
        assertEquals(90.0, metrics.offloadPercent());
    }

    @Test
    void telemetryHasNoApiKey() {
        SeedessClient client = new SeedessClient("key_test", baseUrl);
        client.sendTelemetry(new TelemetryInput("s", "demo"));
        assertEquals("/api/v1/telemetry", path.get());
        assertNull(auth.get());
    }

    @Test
    void rejectsPublicToken() {
        assertThrows(IllegalArgumentException.class, () -> new SeedessClient("pk_stream"));
    }
}
