package bm.b0b0b0.soulCrates.hook.worldguard;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WorldGuardBypassContext {

    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    private WorldGuardBypassContext() {
    }

    public static void mark(UUID playerId) {
        if (playerId != null) {
            ACTIVE.add(playerId);
        }
    }

    public static boolean marked(UUID playerId) {
        return playerId != null && ACTIVE.contains(playerId);
    }

    public static void unmark(UUID playerId) {
        if (playerId != null) {
            ACTIVE.remove(playerId);
        }
    }
}
