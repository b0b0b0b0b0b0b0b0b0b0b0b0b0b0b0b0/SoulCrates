package bm.b0b0b0.soulCrates.session;

import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SessionRegistry {

    private final JavaPlugin plugin;
    private final long timeoutSeconds;
    private final Map<UUID, CrateOpeningSession> activeByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerBySession = new ConcurrentHashMap<>();
    private final Set<UUID> bulkLocked = ConcurrentHashMap.newKeySet();

    public SessionRegistry(JavaPlugin plugin, long timeoutSeconds) {
        this.plugin = plugin;
        this.timeoutSeconds = Math.max(1L, timeoutSeconds);
    }

    public Optional<CrateOpeningSession> find(UUID playerId) {
        return Optional.ofNullable(activeByPlayer.get(playerId));
    }

    public boolean hasActive(UUID playerId) {
        CrateOpeningSession session = activeByPlayer.get(playerId);
        return session != null && session.isActive();
    }

    public boolean isBulkLocked(UUID playerId) {
        return bulkLocked.contains(playerId);
    }

    public boolean isBusy(UUID playerId) {
        return hasActive(playerId) || isBulkLocked(playerId);
    }

    public boolean tryBeginBulk(UUID playerId) {
        if (isBusy(playerId)) {
            return false;
        }
        bulkLocked.add(playerId);
        PluginSchedulers.runGlobalLater(plugin, timeoutSeconds * 20L, () -> bulkLocked.remove(playerId));
        return true;
    }

    public void endBulk(UUID playerId) {
        bulkLocked.remove(playerId);
    }

    public void register(CrateOpeningSession session) {
        UUID playerId = session.context().playerId();
        CrateOpeningSession previous = activeByPlayer.putIfAbsent(playerId, session);
        if (previous != null && previous.isActive()) {
            throw new IllegalStateException("Player already has an active opening session");
        }
        playerBySession.put(session.sessionId(), playerId);
        PluginSchedulers.runGlobalLater(plugin, timeoutSeconds * 20L, () -> expireIfSame(playerId, session.sessionId()));
    }

    public void unregister(CrateOpeningSession session) {
        UUID playerId = session.context().playerId();
        activeByPlayer.remove(playerId, session);
        playerBySession.remove(session.sessionId(), playerId);
    }

    public void cancelAll() {
        for (CrateOpeningSession session : activeByPlayer.values()) {
            session.cancel();
        }
        activeByPlayer.clear();
        playerBySession.clear();
        bulkLocked.clear();
    }

    private void expireIfSame(UUID playerId, UUID sessionId) {
        CrateOpeningSession session = activeByPlayer.get(playerId);
        if (session == null || !session.sessionId().equals(sessionId) || !session.isActive()) {
            return;
        }
        session.cancel();
        unregister(session);
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null && player.isOnline()) {
            PluginSchedulers.run(plugin, player, () -> player.closeInventory());
        }
    }
}
