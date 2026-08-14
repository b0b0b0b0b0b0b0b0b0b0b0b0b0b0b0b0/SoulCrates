package bm.b0b0b0.soulCrates.model;

import bm.b0b0b0.soulCrates.config.settings.AlternativeRewardSettings;
import java.util.List;

public record RewardDefinition(
        String id,
        String rarityId,
        double weight,
        String displayName,
        String material,
        int customModelData,
        List<String> grants,
        List<String> commands,
        boolean pityEligible,
        boolean broadcast,
        boolean enabled,
        RewardWinLimits limits
) {

    public int requiredKeys(int crateDefault) {
        if (limits == null || limits.requiredKeys() <= 0) {
            return Math.max(1, crateDefault);
        }
        return limits.requiredKeys();
    }

    public RewardDefinition alternativeAsReward() {
        if (limits == null || limits.alternative() == null || !limits.alternative().enabled) {
            return this;
        }
        AlternativeRewardSettings alt = limits.alternative();
        return new RewardDefinition(
                id + "_alt",
                rarityId,
                weight,
                alt.displayName,
                alt.material,
                -1,
                List.copyOf(alt.grants),
                List.copyOf(alt.commands),
                false,
                false,
                true,
                limits
        );
    }
}
