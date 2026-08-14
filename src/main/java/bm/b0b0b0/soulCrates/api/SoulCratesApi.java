package bm.b0b0b0.soulCrates.api;

import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.service.CrateService;
import bm.b0b0b0.soulCrates.service.key.KeyService;
import bm.b0b0b0.soulCrates.service.player.PlayerDataService;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class SoulCratesApi {

    private final CrateService crateService;

    public SoulCratesApi(CrateService crateService) {
        this.crateService = crateService;
    }

    public boolean isLoaded() {
        return crateService.isLoaded();
    }

    public boolean hasActiveSession(UUID playerId) {
        return crateService.hasActiveSession(playerId);
    }

    public Optional<CrateDefinition> findCrate(String crateId) {
        return crateService.findCrate(crateId);
    }

    public int virtualKeys(UUID playerId, String crateId) {
        return crateService.virtualKeys(playerId, crateId);
    }

    public int totalKeys(Player player, String crateId) {
        return crateService.totalKeys(player, crateId);
    }

    public int opens(UUID playerId, String crateId) {
        PlayerDataService dataService = crateService.playerDataService();
        if (dataService == null) {
            return 0;
        }
        return dataService.opens(playerId, crateId);
    }

    public int pity(UUID playerId, String crateId) {
        PlayerDataService dataService = crateService.playerDataService();
        if (dataService == null) {
            return 0;
        }
        return dataService.pity(playerId, crateId);
    }

    public String lastReward(UUID playerId, String crateId) {
        PlayerDataService dataService = crateService.playerDataService();
        if (dataService == null) {
            return "";
        }
        return dataService.lastReward(playerId, crateId);
    }

    public CompletableFuture<Void> giveVirtualKeys(UUID playerId, String crateId, int amount) {
        KeyService keyService = crateService.keyService();
        if (keyService == null) {
            return CompletableFuture.completedFuture(null);
        }
        return keyService.giveVirtualKeys(playerId, crateId, amount);
    }

    public void openPreview(Player player, String crateId) {
        crateService.openPreview(player, crateId);
    }

    public void beginOpen(Player player, String crateId, Location location) {
        beginOpen(player, crateId, location, 1);
    }

    public void beginOpen(Player player, String crateId, Location location, int amount) {
        crateService.beginOpen(player, crateId, location, amount);
    }

    public void openShop(Player player) {
        crateService.openShop(player);
    }
}
