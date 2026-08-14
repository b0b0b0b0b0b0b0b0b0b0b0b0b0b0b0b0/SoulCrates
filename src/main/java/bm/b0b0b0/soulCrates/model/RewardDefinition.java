package bm.b0b0b0.soulCrates.model;

import java.util.List;

public record RewardDefinition(
        String id,
        double weight,
        String displayName,
        String material,
        int customModelData,
        List<String> grants,
        List<String> commands,
        boolean pityEligible,
        boolean broadcast
) {
}
