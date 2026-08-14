package bm.b0b0b0.soulCrates.animation;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

final class PickArenaLayout {

    static final double RADIUS = 3.0;

    private PickArenaLayout() {
    }

    static Location podLocation(Location center, int index, int count) {
        double angle = (Math.PI * 2.0 * index / count) - (Math.PI / 2.0);
        double offsetX = Math.cos(angle) * RADIUS;
        double offsetZ = Math.sin(angle) * RADIUS;
        return center.clone().add(offsetX, 0.0, offsetZ);
    }

    static Location resolveArenaCenter(Location crateAnchor, Player player) {
        if (crateAnchor != null && crateAnchor.getWorld() != null) {
            return groundStand(crateAnchor.getBlock());
        }
        if (player == null) {
            return null;
        }
        Location feet = player.getLocation();
        if (feet.getWorld() == null) {
            return feet.clone();
        }
        return groundStand(feet.getBlock());
    }

    static Location resolvePlayerCenter(Player player) {
        if (player == null) {
            return null;
        }
        Location feet = player.getLocation();
        if (feet.getWorld() == null) {
            return feet.clone();
        }
        Block ground = feet.getBlock();
        if (ground.isPassable()) {
            ground = ground.getRelative(0, -1, 0);
        }
        return groundStand(ground);
    }

    static boolean isOutsideBoundary(Location center, Location location, double radius) {
        if (center == null || location == null || location.getWorld() == null) {
            return false;
        }
        if (center.getWorld() != location.getWorld()) {
            return true;
        }
        double radiusSq = radius * radius;
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        return dx * dx + dz * dz > radiusSq;
    }

    static Location clampLocation(Location center, Location location, double radius) {
        if (center == null || location == null) {
            return location;
        }
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        double distSq = dx * dx + dz * dz;
        double radiusSq = radius * radius;
        if (distSq <= radiusSq) {
            return location;
        }
        double dist = Math.sqrt(distSq);
        Location clamped = location.clone();
        clamped.setX(center.getX() + dx / dist * radius);
        clamped.setZ(center.getZ() + dz / dist * radius);
        return clamped;
    }

    private static Location groundStand(Block block) {
        Block ground = block;
        if (ground.isPassable()) {
            ground = ground.getRelative(0, -1, 0);
        }
        return ground.getLocation().add(0.5, 1.0, 0.5);
    }
}
