package bm.b0b0b0.soulCrates.service.idle;

import bm.b0b0b0.soulCrates.animation.ParticleEffectUtil;
import bm.b0b0b0.soulCrates.config.settings.IdleDisplaySettings;
import bm.b0b0b0.soulCrates.config.settings.IdleEffectSettings;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class IdleParticleService {

    private final JavaPlugin plugin;
    private IdleDisplaySettings settings;
    private final Map<String, IdleLocationState> active = new ConcurrentHashMap<>();
    private ScheduledTask tickTask;

    public IdleParticleService(JavaPlugin plugin, IdleDisplaySettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        restartTask();
    }

    public void applySettings(IdleDisplaySettings settings) {
        this.settings = settings;
        restartTask();
    }

    public void register(String locationKey, Location anchor, CrateDefinition crate) {
        if (!settings.enabled) {
            return;
        }
        List<IdleEffectSettings> effects = crate.idleEffects();
        if (effects == null || effects.isEmpty()) {
            active.remove(locationKey);
            return;
        }
        active.put(locationKey, new IdleLocationState(anchor.clone(), List.copyOf(effects)));
    }

    public void unregister(String locationKey) {
        active.remove(locationKey);
    }

    public void clear() {
        active.clear();
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        active.clear();
    }

    public boolean hasPerCrateEffects(String locationKey) {
        return active.containsKey(locationKey);
    }

    private void restartTask() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (!settings.enabled) {
            return;
        }
        int interval = Math.max(2, settings.idleEffectTickInterval);
        tickTask = PluginSchedulers.runTimer(plugin, (org.bukkit.entity.Entity) null, interval, interval, this::tickAll);
    }

    private void tickAll() {
        for (IdleLocationState state : active.values()) {
            tickState(state);
        }
    }

    private void tickState(IdleLocationState state) {
        Location anchor = state.anchor;
        if (anchor.getWorld() == null || !isChunkLoaded(anchor)) {
            return;
        }
        Location center = anchor.clone().add(0.5, 1.0, 0.5);
        state.tick++;
        for (IdleEffectSettings effect : state.effects) {
            spawnEffect(center, effect, state.tick);
        }
    }

    private void spawnEffect(Location center, IdleEffectSettings effect, int tick) {
        Particle particle = ParticleEffectUtil.parseParticle(effect.particle);
        org.bukkit.Color color = ParticleEffectUtil.parseBukkitColor(effect.color, org.bukkit.Color.RED);
        Location base = center.clone().add(effect.offsetX, effect.offsetY, effect.offsetZ);
        String pattern = effect.pattern == null ? "DEFAULT" : effect.pattern.toUpperCase(Locale.ROOT);
        int amount = Math.max(1, effect.amount);
        double spread = Math.max(0.0, effect.spread);
        double velocity = Math.max(0.0, effect.velocity);
        switch (pattern) {
            case "CIRCLE" -> spawnCircle(base, particle, color, amount, spread, tick);
            case "STAR" -> spawnStar(base, particle, color, amount, spread, tick);
            case "SQUARE" -> spawnSquare(base, particle, color, amount, spread, tick);
            case "SPIRAL" -> spawnSpiral(base, particle, color, amount, spread, tick);
            case "PULSE" -> spawnPulse(base, particle, color, amount, spread, tick);
            default -> ParticleEffectUtil.spawn(
                    base.getWorld(),
                    base,
                    particle,
                    color,
                    amount,
                    spread,
                    spread * 0.5,
                    spread,
                    velocity
            );
        }
    }

    private void spawnCircle(Location base, Particle particle, org.bukkit.Color color, int amount, double radius, int tick) {
        double angle = tick * 0.25;
        for (int index = 0; index < Math.max(1, amount / 2); index++) {
            double step = angle + (Math.PI * 2 * index / Math.max(1, amount / 2));
            Location point = base.clone().add(Math.cos(step) * radius, 0.0, Math.sin(step) * radius);
            ParticleEffectUtil.spawn(point.getWorld(), point, particle, color, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private void spawnStar(Location base, Particle particle, org.bukkit.Color color, int amount, double radius, int tick) {
        double angle = tick * 0.3;
        for (int spike = 0; spike < 5; spike++) {
            double step = angle + spike * Math.PI * 0.4;
            Location point = base.clone().add(Math.cos(step) * radius, 0.05, Math.sin(step) * radius);
            ParticleEffectUtil.spawn(point.getWorld(), point, particle, color, Math.max(1, amount / 5), 0.02, 0.02, 0.02, 0.0);
        }
    }

    private void spawnSquare(Location base, Particle particle, org.bukkit.Color color, int amount, double radius, int tick) {
        double progress = (tick % 20) / 20.0;
        double x = progress < 0.25 ? radius : progress < 0.5 ? radius : progress < 0.75 ? -radius : -radius;
        double z = progress < 0.25 ? -radius : progress < 0.5 ? radius : progress < 0.75 ? radius : -radius;
        ParticleEffectUtil.spawn(base.getWorld(), base.clone().add(x, 0.0, z), particle, color, amount, 0.02, 0.02, 0.02, 0.0);
    }

    private void spawnSpiral(Location base, Particle particle, org.bukkit.Color color, int amount, double radius, int tick) {
        double angle = tick * 0.35;
        double y = (tick % 16) * 0.05;
        Location point = base.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
        ParticleEffectUtil.spawn(point.getWorld(), point, particle, color, amount, 0.02, 0.02, 0.02, 0.0);
    }

    private void spawnPulse(Location base, Particle particle, org.bukkit.Color color, int amount, double radius, int tick) {
        double pulse = radius * (0.5 + 0.5 * Math.sin(tick * 0.25));
        ParticleEffectUtil.spawn(base.getWorld(), base, particle, color, amount, pulse, 0.1, pulse, 0.0);
    }

    private static boolean isChunkLoaded(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        return world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private static final class IdleLocationState {

        private final Location anchor;
        private final List<IdleEffectSettings> effects;
        private int tick;

        private IdleLocationState(Location anchor, List<IdleEffectSettings> effects) {
            this.anchor = anchor;
            this.effects = effects;
        }
    }
}
