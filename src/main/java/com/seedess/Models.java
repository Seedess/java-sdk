package com.seedess;

import java.util.List;
import java.util.Map;

public final class Models {
    private Models() {}

    public record Playback(
        String format,
        String master,
        String manifest,
        String playlist,
        String signaling,
        String config
    ) {}

    public record Stream(
        String id,
        String title,
        Boolean live,
        String status,
        String format,
        Boolean killSwitch,
        Double targetDuration,
        Integer mediaSequence,
        List<Map<String, Object>> variants,
        Integer segments,
        String updatedAt,
        String publicToken,
        Playback playback
    ) {}

    public record Metrics(
        long cdnBytes,
        long p2pBytes,
        double offloadPercent,
        long hashFailures,
        long fallbacks,
        long rebuffers,
        long peakPeers,
        long sessions
    ) {}

    public record CreateStreamInput(String url, String id, String title, Boolean live) {
        public CreateStreamInput(String url) {
            this(url, null, null, null);
        }
    }

    public record TelemetryInput(
        String sessionId,
        String streamId,
        Long cdnBytes,
        Long p2pBytes,
        Long hashFailures,
        Long fallbacks,
        Long rebuffers,
        Long peakPeers
    ) {
        public TelemetryInput(String sessionId, String streamId) {
            this(sessionId, streamId, null, null, null, null, null, null);
        }
    }
}
