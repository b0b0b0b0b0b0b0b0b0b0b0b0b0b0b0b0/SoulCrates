package bm.b0b0b0.soulCrates.service.hologram;

import bm.b0b0b0.soulCrates.config.settings.HologramSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

final class DecentHologramsBridge {

    private static final Map<String, Object> HOLOGRAMS = new ConcurrentHashMap<>();
    private static volatile boolean available;

    private DecentHologramsBridge() {
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
        try {
            remove(locationKey);
            String name = "soulcrates-" + locationKey.toLowerCase(Locale.ROOT).replace(':', '-');
            Location anchor = location.clone().add(0.5, settings.offsetY, 0.5);
            Class<?> apiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            Method createMethod = apiClass.getMethod("createHologram", String.class, Location.class);
            Object hologram = createMethod.invoke(null, name, anchor);
            Class<?> hologramClass = Class.forName("eu.decentsoftware.holograms.api.holograms.Hologram");
            Method setLineMethod = hologramClass.getMethod("setLine", int.class, String.class);
            List<String> lines = settings.lines == null ? List.of() : settings.lines;
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index).replace("{crate}", crate.displayName()).replace("{crate_id}", crate.id());
                setLineMethod.invoke(hologram, index, line);
            }
            HOLOGRAMS.put(locationKey, hologram);
            return true;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    static void remove(String locationKey) {
        Object hologram = HOLOGRAMS.remove(locationKey);
        if (hologram == null) {
            return;
        }
        try {
            Method deleteMethod = hologram.getClass().getMethod("delete");
            deleteMethod.invoke(hologram);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    static void removeAll() {
        for (String key : new ArrayList<>(HOLOGRAMS.keySet())) {
            remove(key);
        }
    }

    private static boolean ensureAvailable(Plugin plugin) {
        if (available) {
            return true;
        }
        Plugin decent = Bukkit.getPluginManager().getPlugin("DecentHolograms");
        if (decent == null || !decent.isEnabled()) {
            return false;
        }
        try {
            Class.forName("eu.decentsoftware.holograms.api.DHAPI", true, decent.getClass().getClassLoader());
            available = true;
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
