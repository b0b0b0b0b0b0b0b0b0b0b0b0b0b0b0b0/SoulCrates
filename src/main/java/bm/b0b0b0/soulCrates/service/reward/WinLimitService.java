package bm.b0b0b0.soulCrates.service.reward;

import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.model.RewardWinLimits;
import bm.b0b0b0.soulCrates.model.RewardWinStats;
import bm.b0b0b0.soulCrates.model.WinLimitCheck;
import bm.b0b0b0.soulCrates.model.WinLimitVerdict;
import bm.b0b0b0.soulCrates.repository.CrateRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public final class WinLimitService {

    private final CrateRepository repository;
    private final MessageService messageService;

    public WinLimitService(CrateRepository repository, MessageService messageService) {
        this.repository = repository;
        this.messageService = messageService;
    }

    public CompletableFuture<WinLimitCheck> check(Player player, CrateDefinition crate, RewardDefinition reward) {
        if (reward == null || !reward.enabled()) {
            return CompletableFuture.completedFuture(new WinLimitCheck(WinLimitVerdict.DISABLED, 0, 0, 0L));
        }
        RewardWinLimits limits = reward.limits();
        if (limits == null) {
            return CompletableFuture.completedFuture(WinLimitCheck.allowed(0, 0));
        }
        if (limits.isExpired()) {
            return CompletableFuture.completedFuture(new WinLimitCheck(WinLimitVerdict.EXPIRED, 0, 0, 0L));
        }
        WinLimitVerdict permissionVerdict = checkPermissions(player, limits);
        if (permissionVerdict != WinLimitVerdict.ALLOWED) {
            return CompletableFuture.completedFuture(new WinLimitCheck(permissionVerdict, 0, 0, 0L));
        }
        UUID playerId = player.getUniqueId();
        String crateId = crate.id();
        String rewardId = reward.id();
        return repository.loadPlayerRewardWins(playerId, crateId, rewardId).thenCombine(
                repository.loadGlobalRewardWins(crateId, rewardId),
                (playerStats, globalStats) -> evaluate(limits, playerStats, globalStats)
        );
    }

    public CompletableFuture<RewardDefinition> resolveReward(Player player, CrateDefinition crate, RewardDefinition reward) {
        return check(player, crate, reward).thenApply(check -> {
            if (check.allowed()) {
                return reward;
            }
            if (reward.limits() != null && reward.limits().alternative() != null && reward.limits().alternative().enabled) {
                return reward.alternativeAsReward();
            }
            return null;
        });
    }

    public CompletableFuture<Void> recordWin(UUID playerId, String crateId, String rewardId) {
        return repository.recordRewardWin(playerId, crateId, rewardId);
    }

    public void sendBlockedMessage(Player player, WinLimitCheck check) {
        String key = switch (check.verdict()) {
            case PLAYER_LIMIT -> "winlimit-player";
            case GLOBAL_LIMIT -> "winlimit-global";
            case PLAYER_COOLDOWN, GLOBAL_COOLDOWN -> "winlimit-cooldown";
            case EXPIRED -> "winlimit-expired";
            case PERMISSION_DENIED -> "winlimit-permission";
            case DISABLED -> "winlimit-disabled";
            default -> "winlimit-blocked";
        };
        messageService.send(
                player.getUniqueId(),
                key,
                messageService.placeholder("seconds", Long.toString(Math.max(1L, check.cooldownRemainingSeconds())))
        );
    }

    public boolean canSelectByKeyRarity(CrateDefinition crate, RewardDefinition reward) {
        String keyRarity = crate.keys().rarity;
        if (keyRarity == null || keyRarity.isBlank()) {
            return true;
        }
        if (reward.rarityId() == null || reward.rarityId().isBlank()) {
            return true;
        }
        return reward.rarityId().equalsIgnoreCase(keyRarity);
    }

    private WinLimitCheck evaluate(RewardWinLimits limits, RewardWinStats playerStats, RewardWinStats globalStats) {
        if (limits.hasPlayerLimit() && playerStats.wins() >= limits.playerWinLimit()) {
            return new WinLimitCheck(WinLimitVerdict.PLAYER_LIMIT, playerStats.wins(), globalStats.wins(), 0L);
        }
        if (limits.hasGlobalLimit() && globalStats.wins() >= limits.globalWinLimit()) {
            return new WinLimitCheck(WinLimitVerdict.GLOBAL_LIMIT, playerStats.wins(), globalStats.wins(), 0L);
        }
        long now = System.currentTimeMillis();
        if (limits.winLimitCooldownSeconds() > 0 && playerStats.lastWinAt() > 0L) {
            long elapsed = (now - playerStats.lastWinAt()) / 1000L;
            if (elapsed < limits.winLimitCooldownSeconds()) {
                return new WinLimitCheck(
                        WinLimitVerdict.PLAYER_COOLDOWN,
                        playerStats.wins(),
                        globalStats.wins(),
                        limits.winLimitCooldownSeconds() - elapsed
                );
            }
        }
        if (limits.globalWinLimitCooldownSeconds() > 0 && globalStats.lastWinAt() > 0L) {
            long elapsed = (now - globalStats.lastWinAt()) / 1000L;
            if (elapsed < limits.globalWinLimitCooldownSeconds()) {
                return new WinLimitCheck(
                        WinLimitVerdict.GLOBAL_COOLDOWN,
                        playerStats.wins(),
                        globalStats.wins(),
                        limits.globalWinLimitCooldownSeconds() - elapsed
                );
            }
        }
        return WinLimitCheck.allowed(playerStats.wins(), globalStats.wins());
    }

    private WinLimitVerdict checkPermissions(Player player, RewardWinLimits limits) {
        List<String> required = limits.requiredPermissions();
        if (required != null) {
            for (String permission : required) {
                if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) {
                    return WinLimitVerdict.PERMISSION_DENIED;
                }
            }
        }
        List<String> restricted = limits.restrictedPermissions();
        if (restricted != null) {
            for (String permission : restricted) {
                if (permission != null && !permission.isBlank() && player.hasPermission(permission)) {
                    return WinLimitVerdict.PERMISSION_DENIED;
                }
            }
        }
        return WinLimitVerdict.ALLOWED;
    }
}
