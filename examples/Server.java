import com.fasterxml.jackson.databind.ObjectMapper;
import com.seedess.Models.CreateStreamInput;
import com.seedess.Models.Stream;
import com.seedess.SeedessClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Full working Seedess example for Java. */
public final class Server {
    private static final String ORIGIN = "https://seedess.com";
    private static final ObjectMapper JSON = new ObjectMapper();

    private static Map<String, Object> cached;

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("SEEDESS_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Set SEEDESS_API_KEY (a server key, key_...) before running this example.");
            System.exit(1);
        }

        String baseUrl = env("SEEDESS_API_BASE", ORIGIN);
        String streamUrl = env("SEEDESS_STREAM_URL", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8");
        String streamId = env("SEEDESS_STREAM_ID", "sdk-example");
        int port = Integer.parseInt(env("PORT", "8787"));

        SeedessClient client = new SeedessClient(apiKey, baseUrl);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

        server.createContext("/api/seedess", exchange -> {
            try {
                synchronized (Server.class) {
                    if (cached == null) {
                        Stream stream = client.createStream(new CreateStreamInput(streamUrl, streamId, "Seedess Java SDK example", null));
                        String token = stream.publicToken();
                        Map<String, Object> playback = new LinkedHashMap<>();
                        playback.put("master", ORIGIN + stream.playback().master());
                        playback.put("signaling", ORIGIN + stream.playback().signaling());
                        playback.put("config", ORIGIN + "/signaling/config?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
                        cached = new LinkedHashMap<>();
                        cached.put("ok", true);
                        cached.put("stream", stream.id());
                        cached.put("token", token);
                        cached.put("playback", playback);
                    }
                }
                send(exchange, 200, "application/json", JSON.writeValueAsBytes(cached));
            } catch (Exception error) {
                send(exchange, 500, "application/json", JSON.writeValueAsBytes(Map.of("ok", false, "error", error.getMessage())));
            }
        });

        server.createContext("/", exchange -> {
            try {
                byte[] html = Files.readAllBytes(Path.of("examples/public/index.html"));
                send(exchange, 200, "text/html; charset=utf-8", html);
            } catch (IOException error) {
                send(exchange, 404, "application/json", JSON.writeValueAsBytes(Map.of("ok", false, "error", "Not found")));
            }
        });

        server.start();
        System.out.println("Seedess example on http://localhost:" + port);
        System.out.println("The first /api/seedess request creates the stream.");
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("content-type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
