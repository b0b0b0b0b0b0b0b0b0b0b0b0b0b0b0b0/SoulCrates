package bm.b0b0b0.soulCrates.service.idle;

import bm.b0b0b0.soulCrates.config.settings.IdleDisplaySettings;
import bm.b0b0b0.soulCrates.engine.DisplayComponent;
import bm.b0b0b0.soulCrates.engine.DisplayEngineRegistry;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.service.CrateRegistry;
import bm.b0b0b0.soulCrates.service.location.CrateLocationService;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class IdleCrateDisplayService {

    private final JavaPlugin plugin;
    private IdleDisplaySettings settings;
    private final DisplayEngineRegistry displayEngineRegistry;
    private final CrateRegistry crateRegistry;
    private final CrateLocationService locationService;
    private final Map<String, DisplayComponent> activeDisplays = new ConcurrentHashMap<>();
    private final Set<String> pausedKeys = ConcurrentHashMap.newKeySet();
    private ScheduledTask particleTask;

    public IdleCrateDisplayService(
            JavaPlugin plugin,
            IdleDisplaySettings settings,
            DisplayEngineRegistry displayEngineRegistry,
            CrateRegistry crateRegistry,
            CrateLocationService locationService
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.displayEngineRegistry = displayEngineRegistry;
        this.crateRegistry = crateRegistry;
        this.locationService = locationService;
    }

    public void applySettings(IdleDisplaySettings settings) {
        this.settings = settings;
        restartParticleTask();
    }

    public void spawnAll() {
        if (!settings.enabled) {
            return;
        }
        despawnAll();
        for (Map.Entry<String, String> entry : locationService.allBindings().entrySet()) {
            spawn(entry.getKey(), entry.getValue());
        }
        restartParticleTask();
    }

    public void spawn(String locationKey, String crateId) {
        if (!settings.enabled || pausedKeys.contains(locationKey)) {
            return;
        }
        Optional<Location> locationOptional = locationService.locationFromKey(locationKey);
        if (locationOptional.isEmpty()) {
            return;
        }
        Location location = locationOptional.get();
        if (!isChunkLoaded(location)) {
            return;
        }
        Optional<CrateDefinition> crateOptional = crateRegistry.find(crateId);
        if (crateOptional.isEmpty()) {
            return;
        }
        despawn(locationKey);
        DisplayComponent component = displayEngineRegistry.createIdleComponent(crateOptional.get(), location);
        component.create();
        if (crateOptional.get().idleAnimation() != null && !crateOptional.get().idleAnimation().isBlank()) {
            component.playAnimation(crateOptional.get().idleAnimation());
        }
        activeDisplays.put(locationKey, component);
    }

    public void despawn(String locationKey) {
        DisplayComponent component = activeDisplays.remove(locationKey);
        if (component != null) {
            component.destroy();
        }
    }

    public void despawnAll() {
        for (String key : activeDisplays.keySet()) {
            despawn(key);
        }
        activeDisplays.clear();
    }

    public void pause(Location location) {
        String key = CrateLocationService.key(location);
        pausedKeys.add(key);
        despawn(key);
    }

    public void resume(Location location) {
        String key = CrateLocationService.key(location);
        pausedKeys.remove(key);
        locationService.findCrateId(location).ifPresent(crateId -> spawn(key, crateId));
    }

    public void onBind(Location location, String crateId) {
        spawn(CrateLocationService.key(location), crateId);
    }

    public void onUnbind(Location location) {
        String key = CrateLocationService.key(location);
        pausedKeys.remove(key);
        despawn(key);
    }

    public void onChunkLoaded(Chunk chunk) {
        if (!settings.enabled) {
            return;
        }
        for (Map.Entry<String, String> entry : locationService.allBindings().entrySet()) {
            Optional<Location> locationOptional = locationService.locationFromKey(entry.getKey());
            if (locationOptional.isEmpty()) {
                continue;
            }
            Location location = locationOptional.get();
            if (location.getWorld() == null || !location.getWorld().equals(chunk.getWorld())) {
                continue;
            }
            if (location.getBlockX() >> 4 != chunk.getX() || location.getBlockZ() >> 4 != chunk.getZ()) {
                continue;
            }
            if (!activeDisplays.containsKey(entry.getKey()) && !pausedKeys.contains(entry.getKey())) {
                spawn(entry.getKey(), entry.getValue());
            }
        }
    }

    public void shutdown() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        despawnAll();
        pausedKeys.clear();
    }

    private void restartParticleTask() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        if (!settings.enabled || !settings.particles) {
            return;
        }
        int interval = Math.max(10, settings.particleIntervalTicks);
        particleTask = PluginSchedulers.runTimer(plugin, (org.bukkit.entity.Entity) null, interval, interval, this::spawnParticles);
    }

    private void spawnParticles() {
        if (!settings.enabled || !settings.particles) {
            return;
        }
        Particle particle = parseParticle(settings.particleType);
        if (particle == null) {
            return;
        }
        int count = Math.max(1, settings.particleCount);
        for (Map.Entry<String, DisplayComponent> entry : activeDisplays.entrySet()) {
            Location anchor = entry.getValue().anchor();
            if (anchor.getWorld() == null || !isChunkLoaded(anchor)) {
                continue;
            }
            Location center = anchor.clone().add(0.5, 1.0, 0.5);
            anchor.getWorld().spawnParticle(particle, center, count, 0.25, 0.15, 0.25, 0.01);
        }
    }

    private static boolean isChunkLoaded(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        return world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private static Particle parseParticle(String raw) {
        if (raw == null || raw.isBlank()) {
            return Particle.PORTAL;
        }
        try {
            return Particle.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return Particle.PORTAL;
        }
    }
}
