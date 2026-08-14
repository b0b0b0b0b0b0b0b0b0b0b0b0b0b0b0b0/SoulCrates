package bm.b0b0b0.soulCrates.model;

import java.util.UUID;
import org.bukkit.Location;

public record OpeningContext(
        UUID playerId,
        String crateId,
        Location crateLocation,
        int keysSpent,
        boolean preview,
        UUID instanceId
) {

    public OpeningContext(UUID playerId, String crateId, Location crateLocation, int keysSpent, boolean preview) {
        this(playerId, crateId, crateLocation, keysSpent, preview, null);
    }
}
