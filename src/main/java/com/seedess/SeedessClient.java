package com.seedess;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seedess.Models.CreateStreamInput;
import com.seedess.Models.Metrics;
import com.seedess.Models.Stream;
import com.seedess.Models.TelemetryInput;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Official Seedess SDK for Java.
 *
 * <p>Server-side only: uses a secret API key ({@code key_…}). Playback uses the
 * separate {@code @seedess/*-p2p} packages and a per-stream public token.
 */
public final class SeedessClient {
    public static final String DEFAULT_BASE_URL = "https://seedess.com";

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public SeedessClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL);
    }

    public SeedessClient(String apiKey, String baseUrl) {
        if (apiKey == null || !apiKey.startsWith("key_")) {
            throw new IllegalArgumentException("Seedess: apiKey must be a server API key (key_…)");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /** Raw JSON request. */
    public JsonNode request(String method, String path, Object body, boolean auth) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + (path.startsWith("/") ? path : "/" + path)))
                .timeout(Duration.ofSeconds(30))
                .header("accept", "application/json");

            if (auth) {
                builder.header("authorization", "Bearer " + apiKey);
            }

            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("content-type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(
                    mapper.writeValueAsString(body), StandardCharsets.UTF_8));
            }

            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode data = response.body() == null || response.body().isEmpty()
                ? null
                : mapper.readTree(response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                JsonNode error = data == null ? null : data.path("error");
                String code = error == null || error.isMissingNode() ? "http_error" : error.path("code").asText("http_error");
                String message = error == null || error.isMissingNode()
                    ? "HTTP " + response.statusCode()
                    : error.path("message").asText("HTTP " + response.statusCode());
                throw new SeedessException(response.statusCode(), code, message);
            }
            return data;
        } catch (IOException error) {
            throw new RuntimeException("Seedess transport error: " + error.getMessage(), error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Seedess request interrupted", error);
        }
    }

    public Stream createStream(CreateStreamInput input) {
        return mapper.convertValue(request("POST", "/api/v1/streams", input, true), Stream.class);
    }

    public Stream createStream(String url) {
        return createStream(new CreateStreamInput(url));
    }

    public Stream getStream(String id) {
        return mapper.convertValue(request("GET", "/api/v1/streams/" + escape(id), null, true), Stream.class);
    }

    public List<Stream> listStreams() {
        JsonNode data = request("GET", "/api/v1/streams", null, true);
        List<Stream> streams = new ArrayList<>();
        for (JsonNode item : data.path("streams")) {
            streams.add(mapper.convertValue(item, Stream.class));
        }
        return streams;
    }

    public JsonNode refreshStream(String id) {
        return request("POST", "/api/v1/streams/" + escape(id) + "/refresh", null, true);
    }

    public JsonNode disablePeers(String id) {
        return request("POST", "/api/v1/streams/" + escape(id) + "/kill", null, true);
    }

    public JsonNode enablePeers(String id) {
        return request("POST", "/api/v1/streams/" + escape(id) + "/revive", null, true);
    }

    public Metrics getMetrics(String id) {
        JsonNode data = request("GET", "/api/v1/streams/" + escape(id) + "/metrics", null, true);
        return mapper.convertValue(data.path("metrics"), Metrics.class);
    }

    public void sendTelemetry(TelemetryInput input) {
        request("POST", "/api/v1/telemetry", input, false);
    }

    private static String escape(String id) {
        return URLEncoder.encode(String.valueOf(id), StandardCharsets.UTF_8).replace("+", "%20");
    }
}
