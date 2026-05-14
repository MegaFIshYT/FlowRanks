package gg.flowpvp.client.model;

public record RankedStats(
        int totalRating,
        int wins,
        int losses,
        int currentStreak,
        int placementMatchesPlayed,
        String currentRank,
        int position
) {
    public static final int UNRANKED_ELO = 800;

    public int games() {
        return wins + losses;
    }

    public int winRatePercent() {
        int games = games();
        return games == 0 ? 0 : Math.round((wins * 100.0f) / games);
    }

    public RankedTier tier() {
        RankedTier rankedTier = RankedTier.fromId(currentRank);
        return rankedTier != null ? rankedTier : RankedTier.fromElo(totalRating);
    }
}
