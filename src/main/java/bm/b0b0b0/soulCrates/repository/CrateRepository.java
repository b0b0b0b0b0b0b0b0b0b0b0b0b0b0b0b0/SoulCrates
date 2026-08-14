package bm.b0b0b0.soulCrates.repository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;

public interface CrateRepository {

    CompletableFuture<Void> migrate();

    CompletableFuture<Integer> loadVirtualKeys(UUID playerId, String crateId);

    CompletableFuture<Void> saveVirtualKeys(UUID playerId, String crateId, int amount);

    CompletableFuture<Integer> loadPityCounter(UUID playerId, String crateId);

    CompletableFuture<Void> savePityCounter(UUID playerId, String crateId, int counter);

    CompletableFuture<Void> incrementPityCounter(UUID playerId, String crateId);

    CompletableFuture<Integer> loadOpens(UUID playerId, String crateId);

    CompletableFuture<Void> incrementOpens(UUID playerId, String crateId);

    CompletableFuture<Map<String, String>> loadAllLocations();

    CompletableFuture<Void> saveLocation(Location location, String crateId);

    CompletableFuture<Void> deleteLocation(Location location);

    CompletableFuture<Map<String, Integer>> loadAllOpens(UUID playerId);

    CompletableFuture<Map<String, Integer>> loadAllPityCounters(UUID playerId);

    CompletableFuture<Map<String, String>> loadLastRewards(UUID playerId);

    CompletableFuture<Void> recordLastReward(UUID playerId, String crateId, String rewardId);

    CompletableFuture<Map<Integer, String>> loadAllNpcBindings();

    CompletableFuture<Void> saveNpcBinding(int npcId, String crateId);

    CompletableFuture<Void> deleteNpcBinding(int npcId);
}
