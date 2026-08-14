package bm.b0b0b0.soulCrates.service.location;

import bm.b0b0b0.soulCrates.repository.CrateRepository;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class CrateLocationService {

    private final CrateRepository repository;
    private final Map<String, String> locationIndex = new ConcurrentHashMap<>();

    public CrateLocationService(CrateRepository repository) {
        this.repository = repository;
    }

    public CompletableFuture<Void> loadAll() {
        return repository.loadAllLocations().thenAccept(entries -> {
            locationIndex.clear();
            locationIndex.putAll(entries);
        });
    }

    public Map<String, String> allBindings() {
        return Collections.unmodifiableMap(locationIndex);
    }

    public Optional<String> findCrateId(Location location) {
        if (location.getWorld() == null) {
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
        String key = key(location);
        locationIndex.put(key, crateId.toLowerCase(Locale.ROOT));
        return repository.saveLocation(location, crateId);
    }

    public CompletableFuture<Void> unbind(Location location) {
        locationIndex.remove(key(location));
        return repository.deleteLocation(location);
    }

    public void clear() {
        locationIndex.clear();
    }

    public static String key(Location location) {
        return location.getWorld().getName().toLowerCase(Locale.ROOT)
                + ":" + location.getBlockX()
                + ":" + location.getBlockY()
                + ":" + location.getBlockZ();
    }
}
