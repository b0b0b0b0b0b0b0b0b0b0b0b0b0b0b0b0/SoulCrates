package bm.b0b0b0.soulCrates.service.claim;

import bm.b0b0b0.soulCrates.config.settings.ClaimSettings;
import bm.b0b0b0.soulCrates.model.PendingClaim;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.repository.CrateRepository;
import bm.b0b0b0.soulCrates.service.reward.RewardDeliveryService;
import bm.b0b0b0.soulCrates.util.RewardSnapshotCodec;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.entity.Player;

public final class ClaimService {

    private final CrateRepository repository;
    private final RewardDeliveryService rewardDeliveryService;
    private final ConcurrentHashMap<UUID, AtomicInteger> pendingCounts = new ConcurrentHashMap<>();
    private ClaimSettings settings;

    public ClaimService(CrateRepository repository, RewardDeliveryService rewardDeliveryService, ClaimSettings settings) {
        this.repository = repository;
        this.rewardDeliveryService = rewardDeliveryService;
        this.settings = settings;
    }

    public void applySettings(ClaimSettings settings) {
        this.settings = settings;
    }

    public boolean enabled() {
        return settings != null && settings.enabled;
    }

    public CompletableFuture<Void> preloadPlayer(UUID playerId) {
        return repository.countPendingClaims(playerId).thenAccept(count -> pendingCounts.put(playerId, new AtomicInteger(Math.max(0, count))));
    }

    public void unloadPlayer(UUID playerId) {
        pendingCounts.remove(playerId);
    }

    public int pendingCount(UUID playerId) {
        AtomicInteger counter = pendingCounts.get(playerId);
        return counter == null ? 0 : Math.max(0, counter.get());
    }

    public CompletableFuture<Long> enqueue(UUID playerId, String crateId, RewardDefinition reward) {
        if (!enabled() || reward == null) {
            return CompletableFuture.completedFuture(-1L);
        }
        return repository.enqueueClaim(playerId, crateId, RewardSnapshotCodec.encode(reward)).thenApply(claimId -> {
            if (claimId > 0) {
                adjustPendingCount(playerId, 1);
            }
            return claimId;
        });
    }

    public CompletableFuture<List<PendingClaim>> loadPending(UUID playerId) {
        return repository.loadPendingClaims(playerId).thenApply(claims -> {
            pendingCounts.put(playerId, new AtomicInteger(claims.size()));
            return claims;
        });
    }

    public CompletableFuture<Integer> countPending(UUID playerId) {
        return CompletableFuture.completedFuture(pendingCount(playerId));
    }

    public CompletableFuture<Boolean> claimOne(Player player, PendingClaim claim) {
        if (claim == null || claim.reward() == null) {
            return CompletableFuture.completedFuture(false);
        }
        UUID playerId = player.getUniqueId();
        return repository.deleteClaim(claim.claimId(), playerId).thenApply(deleted -> {
            if (!deleted) {
                return false;
            }
            adjustPendingCount(playerId, -1);
            rewardDeliveryService.deliverDirect(player, claim.crateId(), claim.reward());
            return true;
        });
    }

    public CompletableFuture<Integer> claimAll(Player player) {
        UUID playerId = player.getUniqueId();
        return loadPending(playerId).thenCompose(claims -> {
            CompletableFuture<Integer> chain = CompletableFuture.completedFuture(0);
            for (PendingClaim claim : claims) {
                chain = chain.thenCompose(count -> claimOne(player, claim).thenApply(success -> success ? count + 1 : count));
            }
            return chain.thenApply(count -> {
                if (count <= 0) {
                    pendingCounts.put(playerId, new AtomicInteger(0));
                }
                return count;
            });
        });
    }

    public CompletableFuture<Void> giveOffline(UUID playerId, String crateId, RewardDefinition reward) {
        if (!enabled() || !settings.offlineSupport) {
            return CompletableFuture.completedFuture(null);
        }
        return enqueue(playerId, crateId, reward).thenApply(ignored -> null);
    }

    private void adjustPendingCount(UUID playerId, int delta) {
        pendingCounts.computeIfAbsent(playerId, ignored -> new AtomicInteger(0)).updateAndGet(value -> Math.max(0, value + delta));
    }
}
