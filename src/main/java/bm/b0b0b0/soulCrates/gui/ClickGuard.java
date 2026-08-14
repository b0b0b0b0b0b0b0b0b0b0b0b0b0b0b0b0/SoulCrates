package bm.b0b0b0.soulCrates.gui;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ClickGuard {

    private final long debounceMillis;
    private final ConcurrentHashMap<UUID, Long> lastClickAt = new ConcurrentHashMap<>();

    public ClickGuard(long debounceMillis) {
        this.debounceMillis = Math.max(0L, debounceMillis);
    }

    public boolean allow(UUID playerId) {
        if (playerId == null || debounceMillis <= 0L) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long previous = lastClickAt.put(playerId, now);
        return previous == null || now - previous >= debounceMillis;
    }

    public static boolean isSoulMenu(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        InventoryHolder holder = inventory.getHolder(false);
        return holder instanceof SoulMenu;
    }
}
