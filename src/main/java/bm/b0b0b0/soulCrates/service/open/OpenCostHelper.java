package bm.b0b0b0.soulCrates.service.open;

import bm.b0b0b0.soulCrates.config.settings.OpenCostSettings;
import bm.b0b0b0.soulCrates.hook.HookRegistry;
import bm.b0b0b0.soulCrates.hook.vault.VaultEconomyHook;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.service.key.KeyService;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public final class OpenCostHelper {

    private OpenCostHelper() {
    }

    public static CompletableFuture<Boolean> consumeAccess(
            Player player,
            CrateDefinition crate,
            int keysRequired,
            KeyService keyService,
            HookRegistry hookRegistry,
            MessageService messageService
    ) {
        if (crate.opening().requireKey) {
            int needed = Math.max(1, keysRequired);
            if (keyService.totalKeys(player, crate.id()) >= needed) {
                return keyService.consumeForOpen(player, crate, needed);
            }
            OpenCostSettings openCost = crate.opening().openCost;
            if (openCost != null && openCost.enabled && openCost.vaultPrice > 0.0) {
                return chargeVault(player, crate, openCost, hookRegistry, messageService);
            }
            return CompletableFuture.completedFuture(false);
        }
        OpenCostSettings openCost = crate.opening().openCost;
        if (openCost != null && openCost.enabled && openCost.vaultPrice > 0.0) {
            return chargeVault(player, crate, openCost, hookRegistry, messageService);
        }
        return CompletableFuture.completedFuture(true);
    }

    private static CompletableFuture<Boolean> chargeVault(
            Player player,
            CrateDefinition crate,
            OpenCostSettings openCost,
            HookRegistry hookRegistry,
            MessageService messageService
    ) {
        double price = Math.max(0.0, openCost.vaultPrice);
        if (price <= 0.0) {
            return CompletableFuture.completedFuture(true);
        }
        return hookRegistry.findHook(VaultEconomyHook.class)
                .map(vault -> {
                    if (!vault.has(player.getUniqueId(), price)) {
                        messageService.send(player.getUniqueId(), "open-no-money");
                        return CompletableFuture.completedFuture(false);
                    }
                    boolean withdrawn = vault.withdraw(player.getUniqueId(), price);
                    if (!withdrawn) {
                        messageService.send(player.getUniqueId(), "open-no-money");
                    }
                    return CompletableFuture.completedFuture(withdrawn);
                })
                .orElseGet(() -> {
                    messageService.send(player.getUniqueId(), "open-no-money");
                    return CompletableFuture.completedFuture(false);
                });
    }
}
