package bm.b0b0b0.soulCrates.service.location;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

public final class CrateLocationService {

    private final File storageFile;
    private final ExecutorService ioExecutor;
    private final Map<String, String> locationIndex = new ConcurrentHashMap<>();

    public CrateLocationService(Path dataFolderPath) {
        this.storageFile = dataFolderPath.resolve("data").resolve("crate-locations.yml").toFile();
        this.ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "SoulCrates-Locations");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<Void> loadAll() {
        return CompletableFuture.runAsync(() -> {
            ensureStorageReady();
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(storageFile);
            Map<String, String> loaded = new ConcurrentHashMap<>();
            List<Map<?, ?>> bindings = yaml.getMapList("bindings");
            for (Map<?, ?> entry : bindings) {
                String world = stringValue(entry.get("world"));
                String crateId = stringValue(entry.get("crate"));
                Integer x = intValue(entry.get("x"));
                Integer y = intValue(entry.get("y"));
                Integer z = intValue(entry.get("z"));
                if (world == null || crateId == null || x == null || y == null || z == null) {
                    continue;
                }
                String key = key(world, x, y, z);
                if (key == null) {
                    continue;
                }
                loaded.put(key, crateId.toLowerCase(Locale.ROOT));
            }
            locationIndex.clear();
            locationIndex.putAll(loaded);
        }, ioExecutor);
    }

    public Map<String, String> allBindings() {
        return Collections.unmodifiableMap(locationIndex);
    }

    public Optional<String> findCrateId(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(locationIndex.get(key(location)));
    }

    public Optional<Location> locationFromKey(String locationKey) {
        if (locationKey == null || locationKey.isBlank()) {
            return Optional.empty();
        }
        String[] parts = locationKey.split(":");
        if (parts.length != 4) {
            return Optional.empty();
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            for (World loaded : Bukkit.getWorlds()) {
                if (loaded.getName().equalsIgnoreCase(parts[0])) {
                    world = loaded;
                    break;
                }
            }
        }
        if (world == null) {
            return Optional.empty();
        }
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return Optional.of(new Location(world, x, y, z));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public CompletableFuture<Void> bind(Location location, String crateId) {
        if (location == null || location.getWorld() == null || crateId == null || crateId.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        String key = key(location);
        if (key == null) {
            return CompletableFuture.completedFuture(null);
        }
        locationIndex.put(key, crateId.toLowerCase(Locale.ROOT));
        return persistSnapshot();
    }

    public CompletableFuture<Void> unbind(Location location) {
        if (location == null || location.getWorld() == null) {
            return CompletableFuture.completedFuture(null);
        }
        String key = key(location);
        if (key == null) {
            return CompletableFuture.completedFuture(null);
        }
        locationIndex.remove(key);
        return persistSnapshot();
    }

    public void clear() {
        locationIndex.clear();
    }

    public void shutdown() {
        ioExecutor.shutdownNow();
    }

    public static String key(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return key(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    private CompletableFuture<Void> persistSnapshot() {
        Map<String, String> snapshot = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(locationIndex.keySet());
        keys.sort(String::compareTo);
        for (String key : keys) {
            snapshot.put(key, locationIndex.get(key));
        }
        return CompletableFuture.runAsync(() -> saveSnapshot(snapshot), ioExecutor);
    }

    private void saveSnapshot(Map<String, String> snapshot) {
        ensureStorageReady();
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, String> entry : snapshot.entrySet()) {
            String[] parts = entry.getKey().split(":");
            if (parts.length != 4) {
                continue;
            }
            Integer x = parseInt(parts[1]);
            Integer y = parseInt(parts[2]);
            Integer z = parseInt(parts[3]);
            if (x == null || y == null || z == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("world", parts[0]);
            row.put("x", x);
            row.put("y", y);
            row.put("z", z);
            row.put("crate", entry.getValue());
            rows.add(row);
        }
        yaml.set("bindings", rows);
        try {
            yaml.save(storageFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save crate locations file", exception);
        }
    }

    private void ensureStorageReady() {
        File parent = storageFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create locations directory: " + parent.getAbsolutePath());
        }
        if (!storageFile.exists()) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("bindings", List.of());
            try {
                yaml.save(storageFile);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to initialize crate locations file", exception);
            }
        }
    }

    private static String key(String worldName, int x, int y, int z) {
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        return worldName.toLowerCase(Locale.ROOT).trim() + ":" + x + ":" + y + ":" + z;
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String raw = value.toString().trim();
        return raw.isEmpty() ? null : raw;
    }

    private static Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        return parseInt(value.toString());
    }

    private static Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
