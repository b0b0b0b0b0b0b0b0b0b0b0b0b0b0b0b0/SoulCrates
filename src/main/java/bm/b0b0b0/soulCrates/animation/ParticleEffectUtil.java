package bm.b0b0b0.soulCrates.animation;

import java.awt.Color;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class ParticleEffectUtil {

    private ParticleEffectUtil() {
    }

    public static org.bukkit.Color parseBukkitColor(String raw, org.bukkit.Color fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() != 6) {
            return fallback;
        }
        try {
            int rgb = Integer.parseInt(normalized, 16);
            return org.bukkit.Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static Particle parseParticle(String raw) {
        if (raw == null || raw.isBlank()) {
            return Particle.DUST;
        }
        String normalized = raw.trim().toUpperCase().replace('-', '_');
        if ("SMOKE_NORMAL".equals(normalized)) {
            return Particle.SMOKE;
        }
        try {
            return Particle.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return Particle.DUST;
        }
    }

    public static Location crateCenter(Player player, CrateOpeningSession session) {
        if (session.context().crateLocation() != null && session.context().crateLocation().getWorld() != null) {
            return session.context().crateLocation().clone().add(0.5, 1.0, 0.5);
        }
        return player.getLocation().clone().add(0.0, 1.0, 0.0);
    }

    public static Location offset(Location base, double x, double y, double z) {
        return base.clone().add(x, y, z);
    }

    public static void spawn(
            World world,
            Location location,
            Particle particle,
            org.bukkit.Color color,
            int count,
            double spreadX,
            double spreadY,
            double spreadZ,
            double speed
    ) {
        if (world == null || location == null || count <= 0) {
            return;
        }
        if (particle == Particle.DUST || particle.name().contains("DUST")) {
            world.spawnParticle(
                    Particle.DUST,
                    location,
                    count,
                    spreadX,
                    spreadY,
                    spreadZ,
                    speed,
                    new Particle.DustOptions(color == null ? org.bukkit.Color.WHITE : color, 1.0f)
            );
            return;
        }
        world.spawnParticle(particle, location, count, spreadX, spreadY, spreadZ, speed);
    }

    public static Color awtColor(org.bukkit.Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue());
    }
}
