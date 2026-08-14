package bm.b0b0b0.soulCrates.repository;

import bm.b0b0b0.soulCrates.model.CrateInstance;
import bm.b0b0b0.soulCrates.model.PendingClaim;
import bm.b0b0b0.soulCrates.model.RewardWinStats;
import bm.b0b0b0.soulCrates.model.WinnerEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    CompletableFuture<Long> enqueueClaim(UUID playerId, String crateId, String rewardJson);

    CompletableFuture<List<PendingClaim>> loadPendingClaims(UUID playerId);

    CompletableFuture<Integer> countPendingClaims(UUID playerId);

    CompletableFuture<Boolean> deleteClaim(long claimId, UUID playerId);

    CompletableFuture<Void> recordWinner(String crateId, UUID playerId, String playerName, String rewardId, String rewardDisplay, long wonAt, int maxHistory);

    CompletableFuture<List<WinnerEntry>> loadWinnerHistory(String crateId, int limit);

    CompletableFuture<RewardWinStats> loadPlayerRewardWins(UUID playerId, String crateId, String rewardId);

    CompletableFuture<RewardWinStats> loadGlobalRewardWins(String crateId, String rewardId);

    CompletableFuture<Void> recordRewardWin(UUID playerId, String crateId, String rewardId);

    CompletableFuture<Void> createInstance(UUID instanceId, String crateId, UUID ownerId, long createdAt);

    CompletableFuture<Optional<CrateInstance>> findInstance(UUID instanceId);

    CompletableFuture<List<CrateInstance>> loadPlacedInstances();

    CompletableFuture<Boolean> tryPlaceInstance(UUID instanceId, UUID ownerId, Location location);

    CompletableFuture<Boolean> tryBeginInstanceOpen(UUID instanceId, UUID playerId);

    CompletableFuture<Boolean> tryFinishInstanceOpen(UUID instanceId);

    CompletableFuture<Boolean> tryCancelInstanceOpen(UUID instanceId);

    CompletableFuture<Boolean> tryUnplaceInstance(UUID instanceId, UUID playerId, Location location);

    CompletableFuture<Optional<CrateInstance>> findInstanceAt(Location location);
}
