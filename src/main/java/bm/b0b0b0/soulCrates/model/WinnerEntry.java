package bm.b0b0b0.soulCrates.model;

import java.util.UUID;

public record WinnerEntry(
        String crateId,
        UUID playerId,
        String playerName,
        String rewardId,
        String rewardDisplay,
        long wonAt
) {
}
