package bm.b0b0b0.soulCrates.service.hologram;

import bm.b0b0b0.soulCrates.config.settings.HologramSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.service.CrateRegistry;
import bm.b0b0b0.soulCrates.service.winner.LastWinnerService;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrateHologramService {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final CrateRegistry crateRegistry;
    private final LastWinnerService lastWinnerService;
    private HologramSettings settings;
    private final Map<String, VanillaHologramState> vanillaHolograms = new ConcurrentHashMap<>();
    private final Set<String> pausedKeys = ConcurrentHashMap.newKeySet();
    private ScheduledTask viewerRefreshTask;

    public CrateHologramService(
            JavaPlugin plugin,
            MessageService messageService,
            CrateRegistry crateRegistry,
            HologramSettings settings,
            LastWinnerService lastWinnerService
    ) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.crateRegistry = crateRegistry;
        this.settings = settings;
        this.lastWinnerService = lastWinnerService;
    }

    public void applySettings(HologramSettings settings) {
        this.settings = settings;
        respawnAll();
    }

    public void spawn(String locationKey, String crateId, Location location) {
        if (!settings.enabled || pausedKeys.contains(locationKey)) {
            return;
        }
        if (crateRegistry.find(crateId).isEmpty() || location.getWorld() == null) {
            return;
        }
        despawn(locationKey);
        CrateDefinition crate = crateRegistry.find(crateId).orElseThrow();
        if ("DECENT_HOLOGRAMS".equalsIgnoreCase(settings.provider)) {
            if (DecentHologramsBridge.spawn(plugin, locationKey, location, settings, crate, messageService)) {
                return;
            }
        }
        if ("FANCY_HOLOGRAMS".equalsIgnoreCase(settings.provider)) {
            if (FancyHologramsBridge.spawn(plugin, locationKey, location, settings, crate, messageService)) {
                return;
            }
        }
        spawnVanilla(locationKey, location, crate);
    }

    public void despawn(String locationKey) {
        DecentHologramsBridge.remove(locationKey);
        FancyHologramsBridge.remove(locationKey);
        VanillaHologramState state = vanillaHolograms.remove(locationKey);
        if (state != null) {
            removeAllViewerDisplays(state);
        }
    }

    public void despawnAll() {
        for (String key : new ArrayList<>(vanillaHolograms.keySet())) {
            despawn(key);
        }
        vanillaHolograms.clear();
        DecentHologramsBridge.removeAll();
        FancyHologramsBridge.removeAll();
        stopViewerRefreshTask();
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
        List<String> lines = settings.lines == null ? List.of() : List.copyOf(settings.lines);
        vanillaHolograms.put(locationKey, new VanillaHologramState(location.clone(), crate, lines));
        ensureViewerRefreshTask();
        refreshVanillaHologram(locationKey);
    }

    private void ensureViewerRefreshTask() {
        if (viewerRefreshTask != null) {
            return;
        }
        viewerRefreshTask = PluginSchedulers.runTimer(plugin, (Entity) null, 20L, 20L, this::refreshAllVanillaHolograms);
    }

    private void stopViewerRefreshTask() {
        if (viewerRefreshTask != null) {
            viewerRefreshTask.cancel();
            viewerRefreshTask = null;
        }
    }

    private void refreshAllVanillaHolograms() {
        if (vanillaHolograms.isEmpty()) {
            stopViewerRefreshTask();
            return;
        }
        for (String locationKey : vanillaHolograms.keySet()) {
            refreshVanillaHologram(locationKey);
        }
    }

    private void refreshVanillaHologram(String locationKey) {
        VanillaHologramState state = vanillaHolograms.get(locationKey);
        if (state == null) {
            return;
        }
        World world = state.baseLocation.getWorld();
        if (world == null) {
            removeAllViewerDisplays(state);
            return;
        }
        double range = Math.max(8.0, settings.viewRange);
        double rangeSquared = range * range;
        Set<UUID> nearby = new HashSet<>();
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(state.baseLocation) > rangeSquared) {
                continue;
            }
            nearby.add(player.getUniqueId());
            updateViewerDisplays(state, player);
        }
        for (UUID viewerId : new ArrayList<>(state.viewerDisplays.keySet())) {
            if (!nearby.contains(viewerId)) {
                removeViewerDisplays(state, viewerId);
            }
        }
    }

    private void updateViewerDisplays(VanillaHologramState state, Player player) {
        List<String> lines = state.lineTemplates;
        List<TextDisplay> existing = state.viewerDisplays.get(player.getUniqueId());
        if (existing != null && existing.size() == lines.size() && allAlive(existing)) {
            for (int index = 0; index < lines.size(); index++) {
                TextDisplay display = existing.get(index);
                display.text(messageService.parse(resolveLine(player.getUniqueId(), state, index)));
            }
            return;
        }
        removeViewerDisplays(state, player.getUniqueId());
        World world = state.baseLocation.getWorld();
        if (world == null) {
            return;
        }
        List<TextDisplay> spawned = new ArrayList<>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            final int lineIndex = index;
            Location anchor = state.baseLocation.clone().add(
                    0.5 + settings.offsetX,
                    settings.offsetY - lineIndex * settings.lineSpacing,
                    0.5 + settings.offsetZ
            );
            TextDisplay display = world.spawn(anchor, TextDisplay.class, entity -> configureDisplay(entity, player, state, lineIndex));
            player.showEntity(plugin, display);
            spawned.add(display);
        }
        state.viewerDisplays.put(player.getUniqueId(), spawned);
    }

    private void configureDisplay(TextDisplay entity, Player player, VanillaHologramState state, int lineIndex) {
        entity.text(messageService.parse(resolveLine(player.getUniqueId(), state, lineIndex)));
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
        entity.setVisibleByDefault(false);
        entity.setPersistent(false);
    }

    private String resolveLine(UUID viewerId, VanillaHologramState state, int lineIndex) {
        return HologramLineFormatter.forViewer(
                messageService,
                viewerId,
                state.crate,
                state.lineTemplates.get(lineIndex),
                lastWinnerService,
                crateRegistry
        );
    }

    private static void removeViewerDisplays(VanillaHologramState state, UUID viewerId) {
        List<TextDisplay> displays = state.viewerDisplays.remove(viewerId);
        if (displays == null) {
            return;
        }
        for (TextDisplay display : displays) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
    }

    private static void removeAllViewerDisplays(VanillaHologramState state) {
        for (UUID viewerId : new ArrayList<>(state.viewerDisplays.keySet())) {
            removeViewerDisplays(state, viewerId);
        }
    }

    private static boolean allAlive(List<TextDisplay> displays) {
        for (TextDisplay display : displays) {
            if (display == null || display.isDead()) {
                return false;
            }
        }
        return true;
    }

    private static Display.Billboard parseBillboard(String raw) {
        if (raw == null || raw.isBlank()) {
            return Display.Billboard.CENTER;
        }
        try {
            return Display.Billboard.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
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

    private static final class VanillaHologramState {
        private final Location baseLocation;
        private final CrateDefinition crate;
        private final List<String> lineTemplates;
        private final Map<UUID, List<TextDisplay>> viewerDisplays = new ConcurrentHashMap<>();

        private VanillaHologramState(Location baseLocation, CrateDefinition crate, List<String> lineTemplates) {
            this.baseLocation = baseLocation;
            this.crate = crate;
            this.lineTemplates = lineTemplates;
        }
    }
}
