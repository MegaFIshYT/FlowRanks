package gg.flowpvp.client.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class RankedPlayerData {
    private final String uuid;
    private final String lastKnownName;
    private final Map<Ladder, RankedStats> perLadder;
    private final long lastUpdated;
    private final int globalElo;
    private final int globalPosition;

    public RankedPlayerData(String uuid, String lastKnownName, Map<Ladder, RankedStats> perLadder, long lastUpdated, int globalElo, int globalPosition) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
        EnumMap<Ladder, RankedStats> copy = new EnumMap<>(Ladder.class);
        copy.putAll(perLadder);
        this.perLadder = Collections.unmodifiableMap(copy);
        this.lastUpdated = lastUpdated;
        this.globalElo = globalElo;
        this.globalPosition = globalPosition;
    }

    public static RankedPlayerData empty(String uuid, String name) {
        return new RankedPlayerData(uuid, name, new EnumMap<>(Ladder.class), 0L, RankedStats.UNRANKED_ELO, -1);
    }

    public String uuid() {
        return uuid;
    }

    public String lastKnownName() {
        return lastKnownName;
    }

    public Map<Ladder, RankedStats> perLadder() {
        return perLadder;
    }

    public long lastUpdated() {
        return lastUpdated;
    }

    public int globalElo() {
        return globalElo <= 0 ? RankedStats.UNRANKED_ELO : globalElo;
    }

    public int globalPosition() {
        return globalPosition;
    }

    public RankedStats statsFor(Ladder ladder) {
        return perLadder.get(ladder);
    }

    public int ratingFor(Ladder ladder) {
        if (ladder == Ladder.GLOBAL) {
            return globalElo();
        }

        RankedStats stats = statsFor(ladder);
        return stats == null ? RankedStats.UNRANKED_ELO : stats.totalRating();
    }
}
