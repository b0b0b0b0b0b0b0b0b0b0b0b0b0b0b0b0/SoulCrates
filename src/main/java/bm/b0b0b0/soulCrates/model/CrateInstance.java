package bm.b0b0b0.soulCrates.model;

import java.util.UUID;
import org.bukkit.Location;

public record CrateInstance(
        UUID instanceId,
        String crateId,
        UUID ownerId,
        CrateInstanceState state,
        String world,
        int x,
        int y,
        int z,
        long createdAt,
        long placedAt
) {

    public Location location(org.bukkit.World worldRef) {
        if (worldRef == null || world == null) {
            return null;
        }
        return new Location(worldRef, x, y, z);
    }

    public String serial() {
        String raw = instanceId.toString().replace("-", "");
        return raw.substring(Math.max(0, raw.length() - 8)).toUpperCase();
    }
}
