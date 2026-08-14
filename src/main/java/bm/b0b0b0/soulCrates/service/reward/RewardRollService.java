package bm.b0b0b0.soulCrates.service.reward;

import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.model.RewardRollResult;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class RewardRollService {

    public RewardRollResult roll(CrateDefinition crateDefinition, int pityCounter, boolean forcePity) {
        if (forcePity) {
            Optional<RewardDefinition> pityReward = findReward(crateDefinition, crateDefinition.pity().rewardId);
            if (pityReward.isPresent()) {
                return new RewardRollResult(pityReward.get(), true);
            }
        }
        RewardDefinition reward = weightedRandom(crateDefinition.rewards());
        return new RewardRollResult(reward, false);
    }

    public RewardRollResult reroll(CrateDefinition crateDefinition) {
        return new RewardRollResult(weightedRandom(crateDefinition.rewards()), false);
    }

    public double chancePercent(CrateDefinition crateDefinition, RewardDefinition reward) {
        double total = 0.0;
        for (RewardDefinition entry : crateDefinition.rewards()) {
            total += Math.max(0.0, entry.weight());
        }
        if (total <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, reward.weight()) * 100.0 / total;
    }

    public Optional<RewardDefinition> findReward(CrateDefinition crateDefinition, String rewardId) {
        if (rewardId == null || rewardId.isBlank()) {
            return Optional.empty();
        }
        String normalized = rewardId.toLowerCase(Locale.ROOT);
        for (RewardDefinition reward : crateDefinition.rewards()) {
            if (reward.id().equals(normalized)) {
                return Optional.of(reward);
            }
        }
        return Optional.empty();
    }

    private RewardDefinition weightedRandom(List<RewardDefinition> rewards) {
        if (rewards.isEmpty()) {
            throw new IllegalStateException("Crate has no rewards configured");
        }
        double total = 0.0;
        for (RewardDefinition reward : rewards) {
            total += Math.max(0.0, reward.weight());
        }
        if (total <= 0.0) {
            return rewards.get(0);
        }
        double roll = ThreadLocalRandom.current().nextDouble(total);
        double cursor = 0.0;
        for (RewardDefinition reward : rewards) {
            cursor += Math.max(0.0, reward.weight());
            if (roll <= cursor) {
                return reward;
            }
        }
        return rewards.get(rewards.size() - 1);
    }
}
