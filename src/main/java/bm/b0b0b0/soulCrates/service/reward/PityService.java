package bm.b0b0b0.soulCrates.service.reward;

import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.repository.CrateRepository;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PityService {

    private final CrateRepository repository;

    public PityService(CrateRepository repository) {
        this.repository = repository;
    }

    public CompletableFuture<Integer> loadCounter(UUID playerId, String crateId) {
        return repository.loadPityCounter(playerId, crateId);
    }

    public CompletableFuture<Boolean> shouldForcePity(CrateDefinition crateDefinition, int counter) {
        if (!crateDefinition.pity().enabled) {
            return CompletableFuture.completedFuture(false);
        }
        if (crateDefinition.pity().rewardId == null || crateDefinition.pity().rewardId.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.completedFuture(counter + 1 >= Math.max(1, crateDefinition.pity().threshold));
    }

    public CompletableFuture<Void> afterRoll(UUID playerId, CrateDefinition crateDefinition, boolean pityTriggered, String rewardId) {
        if (!crateDefinition.pity().enabled) {
            return repository.incrementOpens(playerId, crateDefinition.id());
        }
        if (pityTriggered) {
            return repository.savePityCounter(playerId, crateDefinition.id(), 0)
                    .thenCompose(ignored -> repository.incrementOpens(playerId, crateDefinition.id()));
        }
        String pityRewardId = crateDefinition.pity().rewardId.toLowerCase();
        if (rewardId.equals(pityRewardId)) {
            return repository.savePityCounter(playerId, crateDefinition.id(), 0)
                    .thenCompose(ignored -> repository.incrementOpens(playerId, crateDefinition.id()));
        }
        return repository.incrementPityCounter(playerId, crateDefinition.id())
                .thenCompose(ignored -> repository.incrementOpens(playerId, crateDefinition.id()));
    }
}
