package bm.b0b0b0.soulCrates.service.reroll;

import bm.b0b0b0.soulCrates.config.settings.RerollSettings;
import bm.b0b0b0.soulCrates.hook.HookRegistry;
import bm.b0b0b0.soulCrates.hook.vault.VaultEconomyHook;
import bm.b0b0b0.soulCrates.model.RewardRollResult;
import bm.b0b0b0.soulCrates.service.reward.RewardRollService;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import org.bukkit.entity.Player;

public final class RerollService {

    private final RewardRollService rewardRollService;
    private final HookRegistry hookRegistry;

    public RerollService(RewardRollService rewardRollService, HookRegistry hookRegistry) {
        this.rewardRollService = rewardRollService;
        this.hookRegistry = hookRegistry;
    }

    public int rerollsRemaining(CrateOpeningSession session) {
        RerollSettings settings = session.crateDefinition().reroll();
        int max = maxRerollsForReward(session);
        return Math.max(0, max - session.rerollsUsed());
    }

    public int maxRerollsForReward(CrateOpeningSession session) {
        RerollSettings settings = session.crateDefinition().reroll();
        if (!settings.useRarityGroups || settings.groups == null || settings.groups.isEmpty()) {
            return Math.max(0, settings.maxRolls);
        }
        String rarity = session.rollResult().reward().rarityId();
        if (rarity == null || rarity.isBlank()) {
            return Math.max(0, settings.maxRolls);
        }
        for (var group : settings.groups) {
            if (group.rarity != null && group.rarity.equalsIgnoreCase(rarity)) {
                return Math.max(0, group.rerolls);
            }
        }
        return Math.max(0, settings.maxRolls);
    }

    public boolean canReroll(Player player, CrateOpeningSession session) {
        if (!session.crateDefinition().reroll().enabled) {
            return false;
        }
        return rerollsRemaining(session) > 0;
    }

    public boolean chargeForReroll(Player player, CrateOpeningSession session) {
        RerollSettings settings = session.crateDefinition().reroll();
        if (session.rerollsUsed() < settings.freeRolls) {
            return true;
        }
        double cost = settings.vaultCost;
        if (cost <= 0.0) {
            return true;
        }
        return hookRegistry.findHook(VaultEconomyHook.class)
                .map(vault -> vault.has(player.getUniqueId(), cost) && vault.withdraw(player.getUniqueId(), cost))
                .orElse(false);
    }

    public double nextRerollCost(CrateOpeningSession session) {
        RerollSettings settings = session.crateDefinition().reroll();
        if (session.rerollsUsed() < settings.freeRolls) {
            return 0.0;
        }
        return Math.max(0.0, settings.vaultCost);
    }

    public RewardRollResult reroll(CrateOpeningSession session) {
        session.incrementRerollsUsed();
        RerollSettings settings = session.crateDefinition().reroll();
        String rarity = session.rollResult().reward().rarityId();
        RewardRollResult result;
        if (settings.useRarityGroups && rarity != null && !rarity.isBlank()) {
            result = rewardRollService.reroll(session.crateDefinition(), rarity);
        } else {
            result = rewardRollService.reroll(session.crateDefinition());
        }
        session.updateRoll(result);
        return result;
    }
}
