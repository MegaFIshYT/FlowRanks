package gg.flowpvp.client.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.flowpvp.client.model.EloHistoryPoint;
import gg.flowpvp.client.model.Ladder;
import gg.flowpvp.client.model.LeaderboardEntry;
import gg.flowpvp.client.model.MatchSummary;
import gg.flowpvp.client.model.RankedPlayerData;
import gg.flowpvp.client.model.RankedStats;
import gg.flowpvp.client.model.SearchResult;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FlowPvpApi implements AutoCloseable {
    public static final String BASE_URL = "https://flowpvp.gg/api";
    public static final String AVATAR_URL = "https://mc-heads.net/avatar/%s/64";
    public static final String PLAYER_SKIN_URL = "https://mc-heads.net/skin/%s";

    private final ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "FlowPvP API");
        thread.setDaemon(true);
        return thread;
    });

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .executor(executor)
            .build();

    public CompletableFuture<List<LeaderboardEntry>> leaderboard(Ladder ladder, int page) {
        String path = "/leaderboard/" + encode(ladder.apiName) + "?page=" + Math.max(1, page);
        return get(path).thenApply(json -> {
            List<LeaderboardEntry> entries = new ArrayList<>();
            if (!json.isJsonArray()) {
                return entries;
            }

            for (JsonElement element : json.getAsJsonArray()) {
                JsonObject object = element.getAsJsonObject();
                entries.add(new LeaderboardEntry(
                        intValue(object, "position", entries.size() + 1),
                        stringValue(object, "uuid", ""),
                        stringValue(object, "name", "Unknown"),
                        intValue(object, "elo", RankedStats.UNRANKED_ELO)
                ));
            }

            return entries;
        });
    }

    public CompletableFuture<List<SearchResult>> search(String query) {
        String clean = query == null ? "" : query.trim();
        if (clean.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        return get("/search?q=" + encode(clean)).thenApply(json -> {
            List<SearchResult> results = new ArrayList<>();
            if (!json.isJsonArray()) {
                return results;
            }

            for (JsonElement element : json.getAsJsonArray()) {
                JsonObject object = element.getAsJsonObject();
                String uuid = stringValue(object, "uuid", "");
                String username = stringValue(object, "username", stringValue(object, "name", clean));
                if (!uuid.isBlank()) {
                    results.add(new SearchResult(uuid, username));
                }
            }

            return results;
        });
    }

    public CompletableFuture<RankedPlayerData> ranked(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        return get("/ranked/" + encode(uuid)).thenApply(json -> {
            if (json == null || json.isJsonNull()) {
                return null;
            }

            JsonObject object = json.getAsJsonObject();
            Map<Ladder, RankedStats> perLadder = new EnumMap<>(Ladder.class);
            JsonObject ladders = object.has("perLadder") && object.get("perLadder").isJsonObject()
                    ? object.getAsJsonObject("perLadder")
                    : new JsonObject();

            for (Map.Entry<String, JsonElement> entry : ladders.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                Ladder ladder = Ladder.fromApi(entry.getKey());
                if (!ladder.hasRankedStats()) {
                    continue;
                }

                JsonObject stats = entry.getValue().getAsJsonObject();
                perLadder.put(ladder, new RankedStats(
                        intValue(stats, "totalRating", RankedStats.UNRANKED_ELO),
                        intValue(stats, "wins", 0),
                        intValue(stats, "losses", 0),
                        intValue(stats, "currentStreak", 0),
                        intValue(stats, "placementMatchesPlayed", 0),
                        stringOrNull(stats, "currentRank"),
                        intValue(stats, "position", -1)
                ));
            }

            return new RankedPlayerData(
                    stringValue(object, "_id", uuid),
                    stringValue(object, "lastKnownName", "Unknown"),
                    perLadder,
                    longValue(object, "lastUpdated", 0L),
                    globalElo(object, perLadder),
                    intValue(object, "globalPosition", -1)
            );
        });
    }

    public CompletableFuture<List<EloHistoryPoint>> rankedHistory(String uuid, Ladder ladder) {
        if (uuid == null || uuid.isBlank() || !ladder.hasRankedStats()) {
            return CompletableFuture.completedFuture(List.of());
        }

        return get("/ranked-history?playerId=" + encode(uuid) + "&ladder=" + encode(ladder.apiName)).thenApply(json -> {
            List<EloHistoryPoint> history = new ArrayList<>();
            if (!json.isJsonArray()) {
                return history;
            }

            for (JsonElement element : json.getAsJsonArray()) {
                JsonObject object = element.getAsJsonObject();
                int elo = intValue(object, "elo", intValue(object, "srAfter", 0));
                long timestamp = longValue(object, "timestamp", longValue(object, "date", 0L));
                if (timestamp > 0) {
                    history.add(new EloHistoryPoint(elo, timestamp));
                }
            }

            return history;
        });
    }

    public CompletableFuture<List<MatchSummary>> matches(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return CompletableFuture.completedFuture(List.of());
        }

        String normalizedUuid = uuid.toLowerCase();
        return get("/matches/" + encode(uuid) + "?type=RANKED").thenApply(json -> {
            List<MatchSummary> matches = new ArrayList<>();
            if (!json.isJsonArray()) {
                return matches;
            }

            for (JsonElement element : json.getAsJsonArray()) {
                JsonObject object = element.getAsJsonObject();
                boolean won = containsUuid(object.getAsJsonArray("winningPlayers"), normalizedUuid);
                List<String> opponents = opponentNames(object, normalizedUuid, won);
                JsonObject ladder = object.has("ladder") && object.get("ladder").isJsonObject()
                        ? object.getAsJsonObject("ladder")
                        : new JsonObject();

                matches.add(new MatchSummary(
                        stringValue(object, "_id", ""),
                        stringValue(object, "arena", "Unknown arena"),
                        stringValue(ladder, "displayName", stringValue(ladder, "_id", "Unknown")),
                        longValue(object, "endedAt", 0L),
                        won,
                        opponents
                ));
            }

            return matches;
        });
    }

    private CompletableFuture<JsonElement> get(String pathAndQuery) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + pathAndQuery))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "FlowPvP-Client-Mod/1.0")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new FlowPvpApiException("FlowPvP returned HTTP " + response.statusCode());
                    }
                    return JsonParser.parseString(response.body());
                });
    }

    private static List<String> opponentNames(JsonObject match, String playerUuid, boolean playerWon) {
        JsonArray opponentUuids = match.getAsJsonArray(playerWon ? "losingPlayers" : "winningPlayers");
        JsonObject postMatchPlayers = match.has("postMatchPlayers") && match.get("postMatchPlayers").isJsonObject()
                ? match.getAsJsonObject("postMatchPlayers")
                : new JsonObject();

        List<String> names = new ArrayList<>();
        if (opponentUuids == null) {
            return names;
        }

        for (JsonElement element : opponentUuids) {
            String uuid = element.getAsString();
            if (uuid.equalsIgnoreCase(playerUuid)) {
                continue;
            }

            JsonElement player = postMatchPlayers.get(uuid);
            if (player != null && player.isJsonObject()) {
                names.add(stringValue(player.getAsJsonObject(), "lastUsername", uuid.substring(0, Math.min(8, uuid.length()))));
            } else {
                names.add(uuid.substring(0, Math.min(8, uuid.length())));
            }
        }

        return names;
    }

    private static boolean containsUuid(JsonArray array, String uuid) {
        if (array == null) {
            return false;
        }

        for (JsonElement element : array) {
            if (element.getAsString().equalsIgnoreCase(uuid)) {
                return true;
            }
        }

        return false;
    }

    private static int averageRating(Map<Ladder, RankedStats> perLadder) {
        int total = 0;
        int count = 0;
        for (Ladder ladder : Ladder.values()) {
            if (!ladder.hasRankedStats()) {
                continue;
            }

            RankedStats stats = perLadder.get(ladder);
            total += stats == null ? RankedStats.UNRANKED_ELO : stats.totalRating();
            count++;
        }

        return count == 0 ? RankedStats.UNRANKED_ELO : total / count;
    }

    private static int globalElo(JsonObject object, Map<Ladder, RankedStats> perLadder) {
        int calculated = averageRating(perLadder);
        int value = intValue(object, "globalElo", calculated);
        return value <= 0 ? calculated : value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String stringValue(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        return element.getAsString();
    }

    private static String stringOrNull(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return element.getAsString();
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        return element.getAsInt();
    }

    private static long longValue(JsonObject object, String key, long fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        return element.getAsLong();
    }

    @Override
    public void close() throws IOException {
        executor.shutdownNow();
    }

    public static class FlowPvpApiException extends RuntimeException {
        public FlowPvpApiException(String message) {
            super(message);
        }
    }
}
