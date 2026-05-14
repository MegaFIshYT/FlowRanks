package gg.flowpvp.client.model;

import java.util.List;

public record MatchSummary(
        String id,
        String arena,
        String ladderName,
        long endedAt,
        boolean won,
        List<String> opponents
) {
}
