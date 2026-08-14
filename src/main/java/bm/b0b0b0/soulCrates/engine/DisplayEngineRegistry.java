package bm.b0b0b0.soulCrates.engine;

import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.DisplayEngineKind;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class DisplayEngineRegistry {

    private final Map<DisplayEngineKind, DisplayEngine> engines = new EnumMap<>(DisplayEngineKind.class);

    public DisplayEngineRegistry(JavaPlugin plugin) {
        register(new VanillaBlockEngine());
        register(new VanillaDisplayEngine());
        register(new ModelEngineDisplayEngine(plugin));
    }

    public void register(DisplayEngine engine) {
        engines.put(engine.kind(), engine);
    }

    public Optional<DisplayEngine> find(DisplayEngineKind kind) {
        DisplayEngine engine = engines.get(kind);
        if (engine == null || !engine.available()) {
            return Optional.empty();
        }
        return Optional.of(engine);
    }

    public DisplayEngine resolve(CrateDefinition crateDefinition) {
        Optional<DisplayEngine> preferred = find(crateDefinition.engineKind());
        if (preferred.isPresent()) {
            return preferred.get();
        }
        for (DisplayEngineKind kind : DisplayEngineKind.values()) {
            Optional<DisplayEngine> fallback = find(kind);
            if (fallback.isPresent()) {
                return fallback.get();
            }
        }
        throw new IllegalStateException("No display engine available for crate " + crateDefinition.id());
    }

    public DisplayComponent createComponent(CrateDefinition crateDefinition, Location location, Player viewer) {
        return resolve(crateDefinition).createComponent(crateDefinition, location, viewer);
    }

    public DisplayComponent createIdleComponent(CrateDefinition crateDefinition, Location location) {
        return resolve(crateDefinition).createComponent(crateDefinition, location, null);
    }
}
