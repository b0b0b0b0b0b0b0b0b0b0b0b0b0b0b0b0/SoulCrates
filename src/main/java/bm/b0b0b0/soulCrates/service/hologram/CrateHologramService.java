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
import org.bukkit.Color;
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
        List<String> lines = settings.lines == null ? List.of() : settings.lines;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).replace("{crate}", crate.displayName()).replace("{crate_id}", crate.id());
            Location anchor = location.clone().add(
                    0.5 + settings.offsetX,
                    settings.offsetY - index * settings.lineSpacing,
                    0.5 + settings.offsetZ
            );
            TextDisplay display = location.getWorld().spawn(anchor, TextDisplay.class, entity -> {
                entity.text(messageService.parse(line));
                entity.setBillboard(parseBillboard(settings.billboard));
                entity.setSeeThrough(settings.seeThrough);
                entity.setShadowed(settings.shadowed);
                entity.setDefaultBackground(settings.defaultBackground);
                if (settings.defaultBackground) {
                    entity.setBackgroundColor(parseColor(settings.backgroundColor, Color.fromARGB(0, 0, 0, 0)));
                }
                entity.setTextOpacity(settings.textOpacity);
                entity.setViewRange(settings.viewRange);
                entity.setShadowRadius(settings.shadowRadius);
                entity.setShadowStrength(settings.shadowStrength);
                entity.setPersistent(false);
            });
            spawned.add(display);
        }
        activeHolograms.put(locationKey, spawned);
    }

    private static Display.Billboard parseBillboard(String raw) {
        if (raw == null || raw.isBlank()) {
            return Display.Billboard.CENTER;
        }
        try {
            return Display.Billboard.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Display.Billboard.CENTER;
        }
    }

    private static Color parseColor(String raw, Color fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() != 8 && normalized.length() != 6) {
            return fallback;
        }
        try {
            if (normalized.length() == 8) {
                return Color.fromARGB(
                        (int) Long.parseLong(normalized.substring(0, 2), 16),
                        (int) Long.parseLong(normalized.substring(2, 4), 16),
                        (int) Long.parseLong(normalized.substring(4, 6), 16),
                        (int) Long.parseLong(normalized.substring(6, 8), 16)
                );
            }
            return Color.fromRGB(Integer.parseInt(normalized, 16));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private void respawnAll() {
        despawnAll();
    }
}
