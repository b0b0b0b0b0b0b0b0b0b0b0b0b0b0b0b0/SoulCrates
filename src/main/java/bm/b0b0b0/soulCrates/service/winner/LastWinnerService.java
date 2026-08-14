package bm.b0b0b0.soulCrates.service.winner;

import bm.b0b0b0.soulCrates.config.settings.LastWinnerSettings;
import bm.b0b0b0.soulCrates.model.WinnerEntry;
import bm.b0b0b0.soulCrates.repository.CrateRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class LastWinnerService {

    private final CrateRepository repository;
    private LastWinnerSettings settings;
    private final Map<String, List<WinnerEntry>> cache = new ConcurrentHashMap<>();

    public LastWinnerService(CrateRepository repository, LastWinnerSettings settings) {
        this.repository = repository;
        this.settings = settings;
    }

    public void applySettings(LastWinnerSettings settings) {
        this.settings = settings;
    }

    public boolean enabled() {
        return settings != null && settings.enabled;
    }

    public CompletableFuture<Void> record(Player player, String crateId, String rewardId, String rewardDisplay) {
        if (!enabled() || player == null) {
            return CompletableFuture.completedFuture(null);
        }
        long wonAt = System.currentTimeMillis();
        WinnerEntry entry = new WinnerEntry(
                crateId.toLowerCase(Locale.ROOT),
                player.getUniqueId(),
                player.getName(),
                rewardId.toLowerCase(Locale.ROOT),
                rewardDisplay,
                wonAt
        );
        cache.computeIfAbsent(entry.crateId(), ignored -> new ArrayList<>()).add(0, entry);
        trimCache(entry.crateId());
        return repository.recordWinner(
                crateId,
                player.getUniqueId(),
                player.getName(),
                rewardId,
                rewardDisplay,
                wonAt,
                settings.historySize
        );
    }

    public CompletableFuture<Void> preload(String crateId) {
        if (!enabled()) {
            return CompletableFuture.completedFuture(null);
        }
        return repository.loadWinnerHistory(crateId, settings.historySize).thenAccept(entries ->
                cache.put(crateId.toLowerCase(Locale.ROOT), new ArrayList<>(entries))
        );
    }

    public WinnerEntry winner(String crateId, int index) {
        List<WinnerEntry> entries = cache.get(crateId.toLowerCase(Locale.ROOT));
        if (entries == null || index < 1 || index > entries.size()) {
            return null;
        }
        return entries.get(index - 1);
    }

    public String winnerPlayer(String crateId, int index) {
        WinnerEntry entry = winner(crateId, index);
        return entry == null ? "" : entry.playerName();
    }

    public String winnerReward(String crateId, int index) {
        WinnerEntry entry = winner(crateId, index);
        return entry == null ? "" : entry.rewardDisplay();
    }

    public String winnerRewardId(String crateId, int index) {
        WinnerEntry entry = winner(crateId, index);
        return entry == null ? "" : entry.rewardId();
    }

    private void trimCache(String crateId) {
        List<WinnerEntry> entries = cache.get(crateId);
        if (entries == null) {
            return;
        }
        int limit = Math.max(1, Math.min(20, settings.historySize));
        while (entries.size() > limit) {
            entries.remove(entries.size() - 1);
        }
    }
}
