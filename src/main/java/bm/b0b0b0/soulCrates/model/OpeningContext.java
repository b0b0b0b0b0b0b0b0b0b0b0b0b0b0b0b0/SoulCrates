package bm.b0b0b0.soulCrates.model;

import java.util.UUID;
import org.bukkit.Location;

public record OpeningContext(
        UUID playerId,
        String crateId,
        Location crateLocation,
        int keysSpent,
        boolean preview,
        UUID instanceId,
        boolean boundBlock
) {

    public OpeningContext(UUID playerId, String crateId, Location crateLocation, int keysSpent, boolean preview) {
        this(playerId, crateId, crateLocation, keysSpent, preview, null, false);
    }

    public OpeningContext(
            UUID playerId,
            String crateId,
            Location crateLocation,
            int keysSpent,
            boolean preview,
            UUID instanceId
    ) {
        this(playerId, crateId, crateLocation, keysSpent, preview, instanceId, false);
    }
}
