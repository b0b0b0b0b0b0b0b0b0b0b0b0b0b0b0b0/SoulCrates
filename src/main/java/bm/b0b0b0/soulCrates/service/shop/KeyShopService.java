package bm.b0b0b0.soulCrates.service.shop;

import bm.b0b0b0.soulCrates.config.settings.CrateShopSettings;
import bm.b0b0b0.soulCrates.config.settings.ShopEntrySettings;
import bm.b0b0b0.soulCrates.hook.HookRegistry;
import bm.b0b0b0.soulCrates.hook.vault.VaultEconomyHook;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.service.CrateRegistry;
import bm.b0b0b0.soulCrates.service.key.KeyService;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class KeyShopService {

    private CrateShopSettings shopSettings;
    private final HookRegistry hookRegistry;
    private final KeyService keyService;
    private final CrateRegistry crateRegistry;
    private final MessageService messageService;

    public KeyShopService(
            CrateShopSettings shopSettings,
            HookRegistry hookRegistry,
            KeyService keyService,
            CrateRegistry crateRegistry,
            MessageService messageService
    ) {
        this.shopSettings = shopSettings;
        this.hookRegistry = hookRegistry;
        this.keyService = keyService;
        this.crateRegistry = crateRegistry;
        this.messageService = messageService;
    }

    public void applySettings(CrateShopSettings shopSettings) {
        this.shopSettings = shopSettings;
    }

    public CrateShopSettings shopSettings() {
        return shopSettings;
    }

    public boolean purchase(Player player, ShopEntrySettings entry) {
        if (!shopSettings.enabled || entry == null || !entry.enabled) {
            messageService.send(player.getUniqueId(), "shop-disabled");
            return false;
        }
        Optional<CrateDefinition> crateOptional = crateRegistry.find(entry.crateId);
        if (crateOptional.isEmpty()) {
            messageService.send(player.getUniqueId(), "crate-not-found", messageService.placeholder("crate", entry.crateId));
            return false;
        }
        CrateDefinition crate = crateOptional.get();
        if (!crate.keys().virtualKeys) {
            messageService.send(player.getUniqueId(), "shop-virtual-disabled");
            return false;
        }
        if (entry.vaultPrice > 0.0) {
            Optional<VaultEconomyHook> vault = hookRegistry.findHook(VaultEconomyHook.class);
            if (vault.isEmpty() || !vault.get().has(player.getUniqueId(), entry.vaultPrice)) {
                messageService.send(player.getUniqueId(), "shop-no-money");
                return false;
            }
            if (!vault.get().withdraw(player.getUniqueId(), entry.vaultPrice)) {
                messageService.send(player.getUniqueId(), "shop-no-money");
                return false;
            }
        }
        if (entry.itemCost != null && !entry.itemCost.isBlank()) {
            if (!consumeItemCost(player, entry.itemCost)) {
                messageService.send(player.getUniqueId(), "shop-no-items");
                return false;
            }
        }
        keyService.giveVirtualKeys(player.getUniqueId(), crate.id(), Math.max(1, entry.keyAmount));
        messageService.send(
                player.getUniqueId(),
                "shop-purchase-success",
                messageService.placeholder("amount", Integer.toString(Math.max(1, entry.keyAmount))),
                messageService.placeholder("crate", crate.displayName())
        );
        return true;
    }

    private boolean consumeItemCost(Player player, String itemCost) {
        String[] parts = itemCost.split(":");
        if (parts.length != 2) {
            return false;
        }
        Material material = Material.matchMaterial(parts[0].trim());
        if (material == null || material.isAir()) {
            return false;
        }
        int amount;
        try {
            amount = Math.max(1, Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException exception) {
            return false;
        }
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.isEmpty() || stack.getType() != material) {
                continue;
            }
            int remove = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - remove);
            if (stack.getAmount() <= 0) {
                contents[slot] = null;
            }
            remaining -= remove;
        }
        if (remaining > 0) {
            return false;
        }
        player.getInventory().setContents(contents);
        return true;
    }
}
