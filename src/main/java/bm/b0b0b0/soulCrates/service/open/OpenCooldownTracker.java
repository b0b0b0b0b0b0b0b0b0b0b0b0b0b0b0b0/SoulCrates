package bm.b0b0b0.soulCrates.service.open;

import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class OpenCooldownTracker {

    private final MessageService messageService;
    private final Map<UUID, Long> openCooldownUntil = new ConcurrentHashMap<>();

    public OpenCooldownTracker(MessageService messageService) {
        this.messageService = messageService;
    }

    public void clear() {
        openCooldownUntil.clear();
    }

    public boolean check(Player player, CrateDefinition crate) {
        if (crate.opening().cooldownSeconds <= 0) {
            return true;
        }
        Long until = openCooldownUntil.get(player.getUniqueId());
        if (until == null || System.currentTimeMillis() >= until) {
            return true;
        }
        long remaining = Math.max(1L, (until - System.currentTimeMillis() + 999L) / 1000L);
        messageService.send(
                player.getUniqueId(),
                "open-cooldown",
                messageService.placeholder("seconds", Long.toString(remaining))
        );
        return false;
    }

    public void apply(Player player, CrateDefinition crate) {
        if (crate.opening().cooldownSeconds > 0) {
            openCooldownUntil.put(
                    player.getUniqueId(),
                    System.currentTimeMillis() + crate.opening().cooldownSeconds * 1000L
            );
        }
    }
}
