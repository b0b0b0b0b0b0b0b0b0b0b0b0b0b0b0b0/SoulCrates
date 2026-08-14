package bm.b0b0b0.soulCrates.service.hologram;

import bm.b0b0b0.soulCrates.config.settings.HologramSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

final class FancyHologramsBridge {

    private static final Map<String, String> HOLOGRAM_NAMES = new ConcurrentHashMap<>();
    private static volatile boolean available;

    private FancyHologramsBridge() {
    }

    static boolean spawn(
            JavaPlugin plugin,
            String locationKey,
            Location location,
            HologramSettings settings,
            CrateDefinition crate,
            MessageService messageService
    ) {
        if (!ensureAvailable(plugin)) {
            return false;
        }
        remove(locationKey);
        String name = "soulcrates_" + locationKey.toLowerCase(Locale.ROOT).replace(':', '_');
        HOLOGRAM_NAMES.put(locationKey, name);
        return true;
    }

    static void remove(String locationKey) {
        HOLOGRAM_NAMES.remove(locationKey);
    }

    static void removeAll() {
        HOLOGRAM_NAMES.clear();
    }

    private static boolean ensureAvailable(Plugin plugin) {
        if (available) {
            return true;
        }
        Plugin fancy = Bukkit.getPluginManager().getPlugin("FancyHolograms");
        available = fancy != null && fancy.isEnabled();
        return available;
    }
}
