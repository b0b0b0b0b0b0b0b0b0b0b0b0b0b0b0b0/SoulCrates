package bm.b0b0b0.soulCrates.service.open;

import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.model.RewardRollResult;
import bm.b0b0b0.soulCrates.repository.CrateRepository;
import bm.b0b0b0.soulCrates.service.player.PlayerDataService;
import bm.b0b0b0.soulCrates.service.reward.BroadcastService;
import bm.b0b0b0.soulCrates.service.reward.PityService;
import bm.b0b0b0.soulCrates.service.reward.RewardDeliveryService;
import bm.b0b0b0.soulCrates.service.reward.RewardRollService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public final class BulkOpenService {

    private final RewardRollService rewardRollService;
    private final PityService pityService;
    private final RewardDeliveryService rewardDeliveryService;
    private final BroadcastService broadcastService;
    private final PlayerDataService playerDataService;
    private final CrateRepository repository;

    public BulkOpenService(
            RewardRollService rewardRollService,
            PityService pityService,
            RewardDeliveryService rewardDeliveryService,
            BroadcastService broadcastService,
            PlayerDataService playerDataService,
            CrateRepository repository
    ) {
        this.rewardRollService = rewardRollService;
        this.pityService = pityService;
        this.rewardDeliveryService = rewardDeliveryService;
        this.broadcastService = broadcastService;
        this.playerDataService = playerDataService;
        this.repository = repository;
    }

    public CompletableFuture<List<RewardRollResult>> rollSequential(UUID playerId, CrateDefinition crate, int amount) {
        List<RewardRollResult> results = new ArrayList<>();
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (int index = 0; index < amount; index++) {
            chain = chain.thenCompose(ignored -> rollOnce(playerId, crate).thenAccept(results::add));
        }
        return chain.thenApply(ignored -> List.copyOf(results));
    }

    public void deliverAll(Player player, CrateDefinition crate, List<RewardRollResult> rolls) {
        Map<String, Integer> summary = new LinkedHashMap<>();
        UUID playerId = player.getUniqueId();
        String crateId = crate.id();
        for (RewardRollResult roll : rolls) {
            rewardDeliveryService.deliver(player, crateId, roll.reward());
            broadcastService.maybeBroadcast(player, crate, roll.reward());
            playerDataService.incrementOpens(playerId, crateId);
            playerDataService.onRewardRecorded(playerId, crateId, roll.reward().id());
            summary.merge(roll.reward().displayName(), 1, Integer::sum);
        }
        repository.loadPityCounter(playerId, crateId)
                .thenAccept(counter -> playerDataService.onPityUpdated(playerId, crateId, counter));
        if (!rolls.isEmpty()) {
            RewardRollResult last = rolls.get(rolls.size() - 1);
            repository.recordLastReward(playerId, crateId, last.reward().id());
        }
    }

    public String formatSummary(Map<String, Integer> summary) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : summary.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(entry.getKey());
            if (entry.getValue() > 1) {
                builder.append(" x").append(entry.getValue());
            }
        }
        return builder.toString();
    }

    public Map<String, Integer> summarize(List<RewardRollResult> rolls) {
        Map<String, Integer> summary = new LinkedHashMap<>();
        for (RewardRollResult roll : rolls) {
            summary.merge(roll.reward().displayName(), 1, Integer::sum);
        }
        return summary;
    }

    public RewardDefinition topReward(List<RewardRollResult> rolls) {
        RewardDefinition best = rolls.get(0).reward();
        double bestWeight = best.weight();
        for (RewardRollResult roll : rolls) {
            if (roll.reward().weight() < bestWeight) {
                best = roll.reward();
                bestWeight = roll.reward().weight();
            }
        }
        return best;
    }

    private CompletableFuture<RewardRollResult> rollOnce(UUID playerId, CrateDefinition crate) {
        return pityService.loadCounter(playerId, crate.id())
                .thenCompose(counter -> pityService.shouldForcePity(crate, counter)
                        .thenCompose(forcePity -> {
                            RewardRollResult roll = rewardRollService.roll(crate, counter, forcePity);
                            return pityService.afterRoll(playerId, crate, roll.pityTriggered(), roll.reward().id())
                                    .thenApply(ignored -> roll);
                        }));
    }
}
