package bm.b0b0b0.soulCrates.service.reward;

import bm.b0b0b0.soulCrates.hook.HookRegistry;
import bm.b0b0b0.soulCrates.hook.vault.VaultEconomyHook;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import java.util.Locale;
import java.util.Map;
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

    public void deliver(Player player, String crateId, RewardDefinition reward) {
        for (String grant : reward.grants()) {
            deliverGrant(player, grant);
        }
        for (String command : reward.commands()) {
            String parsed = command
                    .replace("{player}", player.getName())
                    .replace("{crate}", crateId)
                    .replace("{reward}", reward.id());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    private void deliverGrant(Player player, String grant) {
        if (grant == null || grant.isBlank()) {
            return;
        }
        String normalized = grant.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("vault:")) {
            double amount = parseDouble(normalized.substring("vault:".length()));
            hookRegistry.findHook(VaultEconomyHook.class).ifPresent(hook -> hook.deposit(player.getUniqueId(), amount));
            return;
        }
        int separator = grant.indexOf(':');
        if (separator <= 0) {
            return;
        }
        Material material = Material.matchMaterial(grant.substring(0, separator));
        if (material == null || material.isAir()) {
            return;
        }
        int amount = (int) Math.max(1L, Math.round(parseDouble(grant.substring(separator + 1))));
        ItemStack stack = new ItemStack(material, amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private static double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }
}
