package bm.b0b0b0.soulCrates.service.reward;

import bm.b0b0b0.soulCrates.config.settings.RarityTierSettings;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.model.RewardRollResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class RewardRollService {

    public RewardRollResult roll(CrateDefinition crateDefinition, int pityCounter, boolean forcePity) {
        return roll(crateDefinition, pityCounter, forcePity, null);
    }

    public RewardRollResult roll(CrateDefinition crateDefinition, int pityCounter, boolean forcePity, String guaranteedRarityId) {
        List<RewardDefinition> pool = enabledRewards(crateDefinition.rewards());
        if (pool.isEmpty()) {
            throw new IllegalStateException("Crate has no enabled rewards configured");
        }
        if (forcePity) {
            Optional<RewardDefinition> pityReward = findReward(crateDefinition, crateDefinition.pity().rewardId);
            if (pityReward.isPresent() && pityReward.get().enabled()) {
                return new RewardRollResult(pityReward.get(), true);
            }
        }
        String normalizedRarity = normalizeRarity(guaranteedRarityId);
        if (normalizedRarity != null) {
            RewardDefinition reward = weightedRandom(filterByRarity(pool, normalizedRarity));
            return new RewardRollResult(reward, false);
        }
        if (crateDefinition.rarities() != null && !crateDefinition.rarities().isEmpty()) {
            RarityTierSettings tier = weightedRarity(crateDefinition.rarities());
            List<RewardDefinition> rarityPool = filterByRarity(pool, tier.id.toLowerCase(Locale.ROOT));
            if (rarityPool.isEmpty()) {
                rarityPool = pool;
            }
            return new RewardRollResult(weightedRandom(rarityPool), false);
        }
        RewardDefinition reward = weightedRandom(pool);
        return new RewardRollResult(reward, false);
    }

    public RewardRollResult reroll(CrateDefinition crateDefinition) {
        return roll(crateDefinition, 0, false, null);
    }

    public RewardRollResult reroll(CrateDefinition crateDefinition, String guaranteedRarityId) {
        return roll(crateDefinition, 0, false, guaranteedRarityId);
    }

    public double chancePercent(CrateDefinition crateDefinition, RewardDefinition reward) {
        if (crateDefinition.rarities() != null && !crateDefinition.rarities().isEmpty()) {
            String rarityId = reward.rarityId() == null ? "" : reward.rarityId().toLowerCase(Locale.ROOT);
            double rarityWeight = rarityWeight(crateDefinition, rarityId);
            double totalRarity = totalRarityWeight(crateDefinition);
            if (totalRarity <= 0.0 || rarityWeight <= 0.0) {
                return flatChance(crateDefinition.rewards(), reward);
            }
            List<RewardDefinition> pool = filterByRarity(crateDefinition.rewards(), rarityId);
            if (pool.isEmpty()) {
                return flatChance(crateDefinition.rewards(), reward);
            }
            double rewardTotal = totalWeight(pool);
            if (rewardTotal <= 0.0) {
                return 0.0;
            }
            return (rarityWeight / totalRarity) * (Math.max(0.0, reward.weight()) / rewardTotal) * 100.0;
        }
        return flatChance(crateDefinition.rewards(), reward);
    }

    public Optional<RewardDefinition> findReward(CrateDefinition crateDefinition, String rewardId) {
        if (rewardId == null || rewardId.isBlank()) {
            return Optional.empty();
        }
        String normalized = rewardId.toLowerCase(Locale.ROOT);
        for (RewardDefinition reward : crateDefinition.rewards()) {
            if (reward.id().equals(normalized) && reward.enabled()) {
                return Optional.of(reward);
            }
        }
        return Optional.empty();
    }

    public Optional<RarityTierSettings> findRarity(CrateDefinition crateDefinition, String rarityId) {
        if (rarityId == null || rarityId.isBlank() || crateDefinition.rarities() == null) {
            return Optional.empty();
        }
        String normalized = rarityId.toLowerCase(Locale.ROOT);
        for (RarityTierSettings tier : crateDefinition.rarities()) {
            if (tier.id != null && tier.id.toLowerCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(tier);
            }
        }
        return Optional.empty();
    }

    private static double flatChance(List<RewardDefinition> rewards, RewardDefinition reward) {
        double total = totalWeight(rewards);
        if (total <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, reward.weight()) * 100.0 / total;
    }

    private static double totalWeight(List<RewardDefinition> rewards) {
        double total = 0.0;
        for (RewardDefinition entry : rewards) {
            total += Math.max(0.0, entry.weight());
        }
        return total;
    }

    private static double totalRarityWeight(CrateDefinition crateDefinition) {
        double total = 0.0;
        for (RarityTierSettings tier : crateDefinition.rarities()) {
            total += Math.max(0.0, tier.weight);
        }
        return total;
    }

    private static double rarityWeight(CrateDefinition crateDefinition, String rarityId) {
        for (RarityTierSettings tier : crateDefinition.rarities()) {
            if (tier.id != null && tier.id.toLowerCase(Locale.ROOT).equals(rarityId)) {
                return Math.max(0.0, tier.weight);
            }
        }
        return 0.0;
    }

    private static List<RewardDefinition> filterByRarity(List<RewardDefinition> rewards, String rarityId) {
        List<RewardDefinition> filtered = new ArrayList<>();
        for (RewardDefinition reward : rewards) {
            String rewardRarity = reward.rarityId() == null ? "" : reward.rarityId().toLowerCase(Locale.ROOT);
            if (rewardRarity.equals(rarityId)) {
                filtered.add(reward);
            }
        }
        return filtered;
    }

    private static RarityTierSettings weightedRarity(List<RarityTierSettings> rarities) {
        double total = 0.0;
        for (RarityTierSettings tier : rarities) {
            total += Math.max(0.0, tier.weight);
        }
        if (total <= 0.0) {
            return rarities.get(0);
        }
        double roll = ThreadLocalRandom.current().nextDouble(total);
        double cursor = 0.0;
        for (RarityTierSettings tier : rarities) {
            cursor += Math.max(0.0, tier.weight);
            if (roll <= cursor) {
                return tier;
            }
        }
        return rarities.get(rarities.size() - 1);
    }

    private RewardDefinition weightedRandom(List<RewardDefinition> rewards) {
        if (rewards.isEmpty()) {
            throw new IllegalStateException("Crate has no rewards configured");
        }
        double total = totalWeight(rewards);
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

    private static List<RewardDefinition> enabledRewards(List<RewardDefinition> rewards) {
        List<RewardDefinition> enabled = new ArrayList<>();
        for (RewardDefinition reward : rewards) {
            if (reward.enabled()) {
                enabled.add(reward);
            }
        }
        return enabled;
    }

    private static String normalizeRarity(String guaranteedRarityId) {
        if (guaranteedRarityId == null || guaranteedRarityId.isBlank()) {
            return null;
        }
        return guaranteedRarityId.toLowerCase(Locale.ROOT);
    }
}
