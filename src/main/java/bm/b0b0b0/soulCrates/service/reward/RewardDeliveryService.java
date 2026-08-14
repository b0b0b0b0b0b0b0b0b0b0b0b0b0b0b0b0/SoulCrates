package bm.b0b0b0.soulCrates.service.reward;

import bm.b0b0b0.soulCrates.config.settings.ClaimSettings;
import bm.b0b0b0.soulCrates.hook.HookRegistry;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.service.claim.ClaimService;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class RewardDeliveryService {

    private final JavaPlugin plugin;
    private final HookRegistry hookRegistry;

    public RewardDeliveryService(JavaPlugin plugin, HookRegistry hookRegistry) {
        this.plugin = plugin;
        this.hookRegistry = hookRegistry;
    }

    public CompletableFuture<DeliveryResult> deliverAsync(
            Player player,
            String crateId,
            RewardDefinition reward,
            ClaimSettings claimSettings,
            ClaimService claimService
    ) {
        if (claimSettings != null && claimSettings.enabled && claimSettings.alwaysToClaim && claimService != null) {
            return claimService.enqueue(player.getUniqueId(), crateId, reward)
                    .thenApply(claimId -> claimId > 0 ? DeliveryResult.queued(reward) : DeliveryResult.failure());
        }
        boolean hasItemGrants = hasItemGrants(reward);
        if (hasItemGrants && claimSettings != null && claimSettings.enabled && claimSettings.overflowToClaim && claimService != null) {
            if (!hasInventorySpace(player, reward)) {
                return claimService.enqueue(player.getUniqueId(), crateId, reward)
                        .thenApply(claimId -> claimId > 0 ? DeliveryResult.queued(reward) : DeliveryResult.failure());
            }
        }
        deliverDirect(player, crateId, reward);
        return CompletableFuture.completedFuture(DeliveryResult.direct());
    }

    public void deliverDirect(Player player, String crateId, RewardDefinition reward) {
        for (String grant : reward.grants()) {
            deliverGrant(player, grant);
        }
        for (String command : reward.commands()) {
            if (command == null || command.isBlank()) {
                continue;
            }
            String parsed = command
                    .replace("{player}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString())
                    .replace("{crate}", crateId)
                    .replace("{reward}", reward.id());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    private boolean hasItemGrants(RewardDefinition reward) {
        for (String grant : reward.grants()) {
            if (isMaterialGrant(grant)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasInventorySpace(Player player, RewardDefinition reward) {
        int neededStacks = 0;
        for (String grant : reward.grants()) {
            if (isMaterialGrant(grant)) {
                neededStacks++;
            }
        }
        if (neededStacks <= 0) {
            return true;
        }
        int emptySlots = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.isEmpty()) {
                emptySlots++;
            }
        }
        return emptySlots >= neededStacks;
    }

    private void deliverGrant(Player player, String grant) {
        if (grant == null || grant.isBlank()) {
            return;
        }
        String normalized = grant.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("vault:") || normalized.startsWith("money:")) {
            plugin.getLogger().warning(
                    "Reward grant '" + grant + "' is ignored. Put money in commands, e.g. eco give {player} 1000"
            );
            return;
        }
        int separator = grant.indexOf(':');
        if (separator <= 0) {
            plugin.getLogger().warning("Invalid reward grant '" + grant + "'. Expected MATERIAL:amount");
            return;
        }
        Material material = Material.matchMaterial(grant.substring(0, separator));
        if (material == null || material.isAir()) {
            plugin.getLogger().warning("Unknown material in reward grant '" + grant + "'");
            return;
        }
        int amount = (int) Math.max(1L, Math.round(parseDouble(grant.substring(separator + 1))));
        ItemStack stack = new ItemStack(material, amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private static boolean isMaterialGrant(String grant) {
        if (grant == null || grant.isBlank()) {
            return false;
        }
        String normalized = grant.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("vault:") || normalized.startsWith("money:")) {
            return false;
        }
        int separator = grant.indexOf(':');
        if (separator <= 0) {
            return false;
        }
        Material material = Material.matchMaterial(grant.substring(0, separator));
        return material != null && !material.isAir();
    }

    private static double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }
}
