package bm.b0b0b0.soulCrates.service.reward;

import bm.b0b0b0.soulCrates.config.settings.ClaimSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardRollResult;
import bm.b0b0b0.soulCrates.repository.CrateRepository;
import bm.b0b0b0.soulCrates.service.claim.ClaimService;
import bm.b0b0b0.soulCrates.service.player.PlayerDataService;
import bm.b0b0b0.soulCrates.service.winner.LastWinnerService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.bukkit.entity.Player;

public final class RewardSettlementService {

    private final MessageService messageService;
    private final RewardDeliveryService rewardDeliveryService;
    private final BroadcastService broadcastService;
    private final PityService pityService;
    private final PlayerDataService playerDataService;
    private final CrateRepository repository;
    private final ClaimService claimService;
    private final LastWinnerService lastWinnerService;
    private final WinLimitService winLimitService;
    private final Supplier<ClaimSettings> claimSettings;

    public RewardSettlementService(
            MessageService messageService,
            RewardDeliveryService rewardDeliveryService,
            BroadcastService broadcastService,
            PityService pityService,
            PlayerDataService playerDataService,
            CrateRepository repository,
            ClaimService claimService,
            LastWinnerService lastWinnerService,
            WinLimitService winLimitService,
            Supplier<ClaimSettings> claimSettings
    ) {
        this.messageService = messageService;
        this.rewardDeliveryService = rewardDeliveryService;
        this.broadcastService = broadcastService;
        this.pityService = pityService;
        this.playerDataService = playerDataService;
        this.repository = repository;
        this.claimService = claimService;
        this.lastWinnerService = lastWinnerService;
        this.winLimitService = winLimitService;
        this.claimSettings = claimSettings;
    }

    public CompletableFuture<DeliveryResult> deliver(Player player, CrateDefinition crate, RewardRollResult roll) {
        return winLimitService.resolveReward(player, crate, roll.reward()).thenCompose(resolved -> {
            if (resolved == null) {
                return CompletableFuture.completedFuture(DeliveryResult.failure());
            }
            RewardRollResult effectiveRoll = resolved.id().equals(roll.reward().id())
                    ? roll
                    : new RewardRollResult(resolved, roll.pityTriggered());
            return rewardDeliveryService.deliverAsync(
                    player,
                    crate.id(),
                    effectiveRoll.reward(),
                    claimSettings.get(),
                    claimService
            ).thenApply(result -> {
                if (result.isFailed()) {
                    return result;
                }
                winLimitService.recordWin(player.getUniqueId(), crate.id(), effectiveRoll.reward().id());
                return result;
            });
        });
    }

    public CompletableFuture<DeliveryResult> deliverWithoutWinLimitCheck(
            Player player,
            CrateDefinition crate,
            RewardRollResult roll
    ) {
        return rewardDeliveryService.deliverAsync(
                player,
                crate.id(),
                roll.reward(),
                claimSettings.get(),
                claimService
        );
    }

    public boolean applyRollStats(Player player, CrateDefinition crate, RewardRollResult roll, DeliveryResult deliveryResult) {
        if (deliveryResult.isFailed()) {
            messageService.send(player.getUniqueId(), "claim-queue-failed");
            return false;
        }
        if (deliveryResult.queued()) {
            messageService.send(player.getUniqueId(), "claim-queued");
        }
        if (!broadcastService.revealBroadcastEnabled()) {
            broadcastService.maybeBroadcast(player, crate, roll.reward());
        }
        UUID playerId = player.getUniqueId();
        String crateId = crate.id();
        pityService.afterRoll(playerId, crate, roll.pityTriggered(), roll.reward().id())
                .thenCompose(ignored -> repository.loadPityCounter(playerId, crateId))
                .thenAccept(counter -> playerDataService.onPityUpdated(playerId, crateId, counter));
        repository.recordLastReward(playerId, crateId, roll.reward().id())
                .thenRun(() -> playerDataService.onRewardRecorded(playerId, crateId, roll.reward().id()));
        if (lastWinnerService != null) {
            lastWinnerService.record(player, crateId, roll.reward().id(), roll.reward().displayName());
        }
        playerDataService.incrementOpens(playerId, crateId);
        messageService.send(
                player.getUniqueId(),
                "open-finished",
                messageService.placeholder("reward", roll.reward().displayName()),
                messageService.placeholder("crate", crate.displayName())
        );
        if (roll.pityTriggered()) {
            messageService.send(player.getUniqueId(), "open-pity-triggered");
        }
        return true;
    }
}
