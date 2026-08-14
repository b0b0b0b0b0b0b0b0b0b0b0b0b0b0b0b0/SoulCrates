package bm.b0b0b0.soulCrates.model;

import java.util.UUID;

public record PendingClaim(
        long claimId,
        UUID playerId,
        String crateId,
        RewardDefinition reward,
        long createdAt
) {
}
