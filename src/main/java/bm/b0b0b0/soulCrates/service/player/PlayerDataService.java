package bm.b0b0b0.soulCrates.service.player;

import bm.b0b0b0.soulCrates.redis.RedisPlayerMirror;
import bm.b0b0b0.soulCrates.repository.CrateRepository;
import bm.b0b0b0.soulCrates.service.key.KeyService;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataService {

    private final CrateRepository repository;
    private final KeyService keyService;
    private final Map<UUID, Map<String, Integer>> opensCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> pityCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> lastRewardCache = new ConcurrentHashMap<>();
    private RedisPlayerMirror mirror;

    public PlayerDataService(CrateRepository repository, KeyService keyService) {
        this.repository = repository;
        this.keyService = keyService;
    }

    public void attachMirror(RedisPlayerMirror mirror) {
        this.mirror = mirror;
    }

    public CompletableFuture<Void> preload(UUID playerId, Collection<String> crateIds) {
        CompletableFuture<Void> keysFuture = keyService.preloadPlayer(playerId, crateIds);
        CompletableFuture<Void> opensFuture = repository.loadAllOpens(playerId).thenAccept(values ->
                opensCache.put(playerId, new ConcurrentHashMap<>(values))
        );
        CompletableFuture<Void> pityFuture = repository.loadAllPityCounters(playerId).thenAccept(values ->
                pityCache.put(playerId, new ConcurrentHashMap<>(values))
        );
        CompletableFuture<Void> historyFuture = repository.loadLastRewards(playerId).thenAccept(values ->
                lastRewardCache.put(playerId, new ConcurrentHashMap<>(values))
        );
        return CompletableFuture.allOf(keysFuture, opensFuture, pityFuture, historyFuture);
    }

    public void unload(UUID playerId) {
        opensCache.remove(playerId);
        pityCache.remove(playerId);
        lastRewardCache.remove(playerId);
        keyService.unloadPlayer(playerId);
    }

    public void clearAll() {
        opensCache.clear();
        pityCache.clear();
        lastRewardCache.clear();
    }

    public int opens(UUID playerId, String crateId) {
        Map<String, Integer> values = opensCache.get(playerId);
        if (values == null) {
            return 0;
        }
        return values.getOrDefault(crateId.toLowerCase(), 0);
    }

    public int pity(UUID playerId, String crateId) {
        Map<String, Integer> values = pityCache.get(playerId);
        if (values == null) {
            return 0;
        }
        return values.getOrDefault(crateId.toLowerCase(), 0);
    }

    public String lastReward(UUID playerId, String crateId) {
        Map<String, String> values = lastRewardCache.get(playerId);
        if (values == null) {
            return "";
        }
        return values.getOrDefault(crateId.toLowerCase(), "");
    }

    public void onOpensIncremented(UUID playerId, String crateId, int total) {
        opensCache.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(crateId.toLowerCase(), total);
    }

    public void onPityUpdated(UUID playerId, String crateId, int counter) {
        applyPityLocal(playerId, crateId, counter);
        if (mirror != null && mirror.enabled()) {
            mirror.publishPity(playerId, crateId, counter);
        }
    }

    public void applyRemotePity(UUID playerId, String crateId, int counter) {
        applyPityLocal(playerId, crateId, counter);
    }

    public CompletableFuture<Void> reloadFromRemote(UUID playerId, Collection<String> crateIds) {
        CompletableFuture<Void> keysFuture = keyService.preloadPlayer(playerId, crateIds);
        CompletableFuture<Void> pityFuture = repository.loadAllPityCounters(playerId).thenAccept(values ->
                pityCache.put(playerId, new ConcurrentHashMap<>(values))
        );
        return CompletableFuture.allOf(keysFuture, pityFuture);
    }

    public void onRewardRecorded(UUID playerId, String crateId, String rewardId) {
        lastRewardCache.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(crateId.toLowerCase(), rewardId.toLowerCase());
    }

    public void incrementOpens(UUID playerId, String crateId) {
        opensCache.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .merge(crateId.toLowerCase(), 1, Integer::sum);
    }

    private void applyPityLocal(UUID playerId, String crateId, int counter) {
        pityCache.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(crateId.toLowerCase(), counter);
    }
}
