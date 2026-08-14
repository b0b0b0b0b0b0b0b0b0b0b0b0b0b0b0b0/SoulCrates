package bm.b0b0b0.soulCrates.model;

import bm.b0b0b0.soulCrates.config.settings.AlternativeRewardSettings;
import java.util.List;

public record RewardWinLimits(
        int playerWinLimit,
        int globalWinLimit,
        int winLimitCooldownSeconds,
        int globalWinLimitCooldownSeconds,
        long expiresAtEpochMs,
        int requiredKeys,
        List<String> requiredPermissions,
        List<String> restrictedPermissions,
        AlternativeRewardSettings alternative
) {

    public boolean hasPlayerLimit() {
        return playerWinLimit >= 0;
    }

    public boolean hasGlobalLimit() {
        return globalWinLimit >= 0;
    }

    public boolean isExpired() {
        return expiresAtEpochMs > 0 && System.currentTimeMillis() >= expiresAtEpochMs;
    }
}
