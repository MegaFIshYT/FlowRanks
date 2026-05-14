package gg.flowpvp.client.api;

import gg.flowpvp.client.model.EloHistoryPoint;
import gg.flowpvp.client.model.Ladder;
import gg.flowpvp.client.model.LeaderboardEntry;
import gg.flowpvp.client.model.MatchSummary;
import gg.flowpvp.client.model.RankedPlayerData;
import gg.flowpvp.client.model.SearchResult;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class FlowPvpCache {
    private static final long LEADERBOARD_TTL = Duration.ofSeconds(45).toMillis();
    private static final long PROFILE_TTL = Duration.ofMinutes(5).toMillis();
    private static final long HISTORY_TTL = Duration.ofMinutes(2).toMillis();
    private static final long MATCH_TTL = Duration.ofMinutes(2).toMillis();
    private static final long MISS_TTL = Duration.ofSeconds(30).toMillis();

    private final FlowPvpApi api;
    private final Map<String, CacheEntry<List<LeaderboardEntry>>> leaderboards = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<RankedPlayerData>> rankedByUuid = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<RankedPlayerData>> rankedByName = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<List<EloHistoryPoint>>> histories = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<List<MatchSummary>>> matches = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<?>> inFlight = new ConcurrentHashMap<>();

    public FlowPvpCache(FlowPvpApi api) {
        this.api = api;
    }

    public CompletableFuture<List<LeaderboardEntry>> leaderboard(Ladder ladder, int page) {
        String key = ladder.apiName + ":" + Math.max(1, page);
        CacheEntry<List<LeaderboardEntry>> cached = leaderboards.get(key);
        if (cached != null && cached.fresh(LEADERBOARD_TTL)) {
            return CompletableFuture.completedFuture(cached.value());
        }

        return dedupe("leaderboard:" + key, api.leaderboard(ladder, page)
                .thenApply(entries -> {
                    leaderboards.put(key, new CacheEntry<>(entries));
                    return entries;
                }));
    }

    public CompletableFuture<RankedPlayerData> rankedByUuid(String uuid, String fallbackName) {
        String normalized = normalize(uuid);
        CacheEntry<RankedPlayerData> cached = rankedByUuid.get(normalized);
        if (cached != null && cached.fresh(cached.value() == null ? MISS_TTL : PROFILE_TTL)) {
            return CompletableFuture.completedFuture(cached.value());
        }

        return dedupe("ranked-uuid:" + normalized, api.ranked(uuid)
                .exceptionally(throwable -> null)
                .thenApply(profile -> {
                    RankedPlayerData result = profile == null && fallbackName != null
                            ? RankedPlayerData.empty(uuid, fallbackName)
                            : profile;
                    rankedByUuid.put(normalized, new CacheEntry<>(result));
                    if (result != null) {
                        rankedByName.put(normalize(result.lastKnownName()), new CacheEntry<>(result));
                    }
                    return result;
                }));
    }

    public CompletableFuture<RankedPlayerData> rankedByName(String name) {
        String normalized = normalize(name);
        CacheEntry<RankedPlayerData> cached = rankedByName.get(normalized);
        if (cached != null && cached.fresh(cached.value() == null ? MISS_TTL : PROFILE_TTL)) {
            return CompletableFuture.completedFuture(cached.value());
        }

        return dedupe("ranked-name:" + normalized, api.search(name)
                .exceptionally(throwable -> List.of())
                .thenCompose(results -> {
                    if (results.isEmpty()) {
                        rankedByName.put(normalized, new CacheEntry<>(null));
                        return CompletableFuture.completedFuture(null);
                    }

                    SearchResult first = results.get(0);
                    return rankedByUuid(first.uuid(), first.username());
                })
                .thenApply(profile -> {
                    rankedByName.put(normalized, new CacheEntry<>(profile));
                    return profile;
                }));
    }

    public CompletableFuture<List<EloHistoryPoint>> history(String uuid, Ladder ladder) {
        String key = normalize(uuid) + ":" + ladder.apiName;
        CacheEntry<List<EloHistoryPoint>> cached = histories.get(key);
        if (cached != null && cached.fresh(HISTORY_TTL)) {
            return CompletableFuture.completedFuture(cached.value());
        }

        return dedupe("history:" + key, api.rankedHistory(uuid, ladder)
                .exceptionally(throwable -> List.of())
                .thenApply(history -> {
                    histories.put(key, new CacheEntry<>(history));
                    return history;
                }));
    }

    public CompletableFuture<List<MatchSummary>> matches(String uuid) {
        String key = normalize(uuid);
        CacheEntry<List<MatchSummary>> cached = matches.get(key);
        if (cached != null && cached.fresh(MATCH_TTL)) {
            return CompletableFuture.completedFuture(cached.value());
        }

        return dedupe("matches:" + key, api.matches(uuid)
                .exceptionally(throwable -> List.of())
                .thenApply(matchList -> {
                    matches.put(key, new CacheEntry<>(matchList));
                    return matchList;
                }));
    }

    @SuppressWarnings("unchecked")
    private <T> CompletableFuture<T> dedupe(String key, CompletableFuture<T> request) {
        CompletableFuture<?> existing = inFlight.putIfAbsent(key, request);
        if (existing != null) {
            return (CompletableFuture<T>) existing;
        }

        request.whenComplete((value, throwable) -> inFlight.remove(key));
        return request;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record CacheEntry<T>(T value, long createdAt) {
        CacheEntry(T value) {
            this(value, System.currentTimeMillis());
        }

        boolean fresh(long ttl) {
            return System.currentTimeMillis() - createdAt <= ttl;
        }
    }
}
