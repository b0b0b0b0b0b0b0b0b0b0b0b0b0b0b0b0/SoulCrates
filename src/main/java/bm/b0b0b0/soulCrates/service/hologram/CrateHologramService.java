package bm.b0b0b0.soulCrates.service.hologram;

import bm.b0b0b0.soulCrates.config.settings.HologramSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.service.CrateRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrateHologramService {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final CrateRegistry crateRegistry;
    private HologramSettings settings;
    private final Map<String, List<Entity>> activeHolograms = new ConcurrentHashMap<>();
    private final Set<String> pausedKeys = ConcurrentHashMap.newKeySet();

    public CrateHologramService(
            JavaPlugin plugin,
            MessageService messageService,
            CrateRegistry crateRegistry,
            HologramSettings settings
    ) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.crateRegistry = crateRegistry;
        this.settings = settings;
    }

    public void applySettings(HologramSettings settings) {
        this.settings = settings;
        respawnAll();
    }

    public void spawn(String locationKey, String crateId, Location location) {
        if (!settings.enabled || pausedKeys.contains(locationKey)) {
            return;
        }
        Optional<CrateDefinition> crateOptional = crateRegistry.find(crateId);
        if (crateOptional.isEmpty() || location.getWorld() == null) {
            return;
        }
        despawn(locationKey);
        if ("DECENT_HOLOGRAMS".equalsIgnoreCase(settings.provider)) {
            if (DecentHologramsBridge.spawn(plugin, locationKey, location, settings, crateOptional.get(), messageService)) {
                activeHolograms.put(locationKey, List.of());
                return;
            }
        }
        if ("FANCY_HOLOGRAMS".equalsIgnoreCase(settings.provider)) {
            if (FancyHologramsBridge.spawn(plugin, locationKey, location, settings, crateOptional.get(), messageService)) {
                activeHolograms.put(locationKey, List.of());
                return;
            }
        }
        spawnVanilla(locationKey, location, crateOptional.get());
    }

    public void despawn(String locationKey) {
        DecentHologramsBridge.remove(locationKey);
        FancyHologramsBridge.remove(locationKey);
        List<Entity> entities = activeHolograms.remove(locationKey);
        if (entities == null) {
            return;
        }
        for (Entity entity : entities) {
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
    }

    public void despawnAll() {
        for (String key : new ArrayList<>(activeHolograms.keySet())) {
            despawn(key);
        }
        activeHolograms.clear();
        DecentHologramsBridge.removeAll();
        FancyHologramsBridge.removeAll();
    }

    public void pause(String locationKey) {
        pausedKeys.add(locationKey);
        despawn(locationKey);
    }

    public void resume(String locationKey, String crateId, Location location) {
        pausedKeys.remove(locationKey);
        spawn(locationKey, crateId, location);
    }

    public void shutdown() {
        despawnAll();
        pausedKeys.clear();
    }

    private void spawnVanilla(String locationKey, Location location, CrateDefinition crate) {
        List<Entity> spawned = new ArrayList<>();
        double yOffset = settings.offsetY;
        List<String> lines = settings.lines == null ? List.of() : settings.lines;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).replace("{crate}", crate.displayName()).replace("{crate_id}", crate.id());
            Location anchor = location.clone().add(0.5, yOffset - index * 0.25, 0.5);
            TextDisplay display = location.getWorld().spawn(anchor, TextDisplay.class, entity -> {
                entity.text(messageService.parse(line));
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setSeeThrough(true);
                entity.setShadowed(true);
                entity.setDefaultBackground(false);
                entity.setPersistent(false);
            });
            spawned.add(display);
        }
        activeHolograms.put(locationKey, spawned);
    }

    private void respawnAll() {
        despawnAll();
    }
}
