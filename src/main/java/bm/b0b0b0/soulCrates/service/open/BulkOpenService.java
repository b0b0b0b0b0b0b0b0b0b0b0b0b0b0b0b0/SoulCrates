package bm.b0b0b0.soulCrates.service.open;

import bm.b0b0b0.soulCrates.config.settings.ClaimSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.model.RewardRollResult;
import bm.b0b0b0.soulCrates.repository.CrateRepository;
import bm.b0b0b0.soulCrates.service.claim.ClaimService;
import bm.b0b0b0.soulCrates.service.player.PlayerDataService;
import bm.b0b0b0.soulCrates.service.reward.BroadcastService;
import bm.b0b0b0.soulCrates.service.reward.DeliveryResult;
import bm.b0b0b0.soulCrates.service.reward.PityService;
import bm.b0b0b0.soulCrates.service.reward.RewardDeliveryService;
import bm.b0b0b0.soulCrates.service.reward.RewardDisplayService;
import bm.b0b0b0.soulCrates.service.reward.RewardRollService;
import bm.b0b0b0.soulCrates.service.winner.LastWinnerService;
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
    private ClaimService claimService;
    private LastWinnerService lastWinnerService;
    private ClaimSettings claimSettings;

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

    public void attachClaim(ClaimService claimService, LastWinnerService lastWinnerService, ClaimSettings claimSettings) {
        this.claimService = claimService;
        this.lastWinnerService = lastWinnerService;
        this.claimSettings = claimSettings;
    }

    public CompletableFuture<List<RewardRollResult>> rollSequential(UUID playerId, CrateDefinition crate, int amount) {
        return rollSequential(playerId, crate, amount, null);
    }

    public CompletableFuture<List<RewardRollResult>> rollSequential(UUID playerId, CrateDefinition crate, int amount, String guaranteedRarity) {
        List<RewardRollResult> results = new ArrayList<>();
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (int index = 0; index < amount; index++) {
            chain = chain.thenCompose(ignored -> rollOnce(playerId, crate, guaranteedRarity).thenAccept(results::add));
        }
        return chain.thenApply(ignored -> List.copyOf(results));
    }

    public CompletableFuture<Void> deliverAll(Player player, CrateDefinition crate, List<RewardRollResult> rolls) {
        if (rolls.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        UUID playerId = player.getUniqueId();
        String crateId = crate.id();
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (RewardRollResult roll : rolls) {
            chain = chain.thenCompose(ignored -> rewardDeliveryService.deliverAsync(
                    player,
                    crateId,
                    roll.reward(),
                    claimSettings,
                    claimService
            ).thenAccept(result -> {
                if (!result.isFailed()) {
                    broadcastService.maybeBroadcast(player, crate, roll.reward());
                    playerDataService.incrementOpens(playerId, crateId);
                }
            }));
        }
        return chain.thenRun(() -> {
            repository.loadPityCounter(playerId, crateId)
                    .thenAccept(counter -> playerDataService.onPityUpdated(playerId, crateId, counter));
            RewardRollResult last = rolls.get(rolls.size() - 1);
            repository.recordLastReward(playerId, crateId, last.reward().id())
                    .thenRun(() -> playerDataService.onRewardRecorded(playerId, crateId, last.reward().id()));
            if (lastWinnerService != null) {
                lastWinnerService.record(player, crateId, last.reward().id(), last.reward().displayName());
            }
        });
    }

    public String formatSummary(MessageService messageService, Player player, String crateId, List<RewardRollResult> rolls) {
        Map<String, Integer> summary = new LinkedHashMap<>();
        for (RewardRollResult roll : rolls) {
            String label = RewardDisplayService.plainText(messageService, player.getUniqueId(), crateId, roll.reward());
            summary.merge(label, 1, Integer::sum);
        }
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
            summary.merge(roll.reward().id(), 1, Integer::sum);
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

    private CompletableFuture<RewardRollResult> rollOnce(UUID playerId, CrateDefinition crate, String guaranteedRarity) {
        return pityService.loadCounter(playerId, crate.id())
                .thenCompose(counter -> pityService.shouldForcePity(crate, counter)
                        .thenCompose(forcePity -> {
                            RewardRollResult roll = rewardRollService.roll(crate, counter, forcePity, guaranteedRarity);
                            return pityService.afterRoll(playerId, crate, roll.pityTriggered(), roll.reward().id())
                                    .thenApply(ignored -> roll);
                        }));
    }
}
