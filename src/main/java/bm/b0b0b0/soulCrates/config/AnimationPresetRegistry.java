package bm.b0b0b0.soulCrates.config;

import bm.b0b0b0.soulCrates.config.settings.AnimationPhaseProperties;
import bm.b0b0b0.soulCrates.config.settings.AnimationPhaseSettings;
import bm.b0b0b0.soulCrates.config.settings.AnimationSettings;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AnimationPresetRegistry {

    private static final Map<String, AnimationSettings> PRESETS = new ConcurrentHashMap<>();

    static {
        register("CLASSIC", phase("default", 40, "#F2F2F2"), phase("swirl", 60, "#404040"), phase("default", 40, "#404040"));
        register("BLAZING", phase("fire", 30, "#ff5500"), phase("fire", 50, "#ffaa00"), phase("firework", 40, "#ff5500"));
        register("KEYSTORM", phase("key_opener", 40, "#ffffff"), phase("swirl", 60, "#a78bfa"), phase("helix", 40, "#c084fc"));
        register("CSGO_STYLE", phase("default", 20, "#6b7280"), phase("csgo", 80, "#404040"), phase("firework", 40, "#fbbf24"));
        register("FIREWORKS", phase("default", 20, "#ffffff"), phase("ball", 50, "#f59e0b"), phase("firework", 40, "#ef4444"));
        register("ARCADE", phase("crack", 25, "#22d3ee"), phase("csgo", 80, "#a855f7"), phase("default", 30, "#34d399"));
        register("BOUNCY", phase("default", 20, "#ffffff"), phase("ball", 60, "#34d399"), phase("default", 30, "#34d399"));
        register("NONE", phase("none", 1, "#ffffff"), phase("none", 1, "#ffffff"), phase("none", 1, "#ffffff"));
    }

    private AnimationPresetRegistry() {
    }

    public static AnimationSettings resolve(String presetId) {
        if (presetId == null || presetId.isBlank()) {
            return null;
        }
        return copy(PRESETS.get(presetId.trim().toUpperCase(Locale.ROOT)));
    }

    public static AnimationSettings resolveOrDefault(AnimationSettings source) {
        if (source == null) {
            return resolve("CLASSIC");
        }
        if (source.preset == null || source.preset.isBlank()) {
            return source;
        }
        AnimationSettings preset = resolve(source.preset);
        return preset == null ? source : preset;
    }

    private static void register(String id, AnimationPhaseSettings first, AnimationPhaseSettings second, AnimationPhaseSettings third) {
        AnimationSettings settings = new AnimationSettings();
        settings.preset = id;
        settings.first = first;
        settings.second = second;
        settings.third = third;
        PRESETS.put(id.toUpperCase(Locale.ROOT), settings);
    }

    private static AnimationPhaseSettings phase(String type, int ticks, String color) {
        AnimationPhaseSettings settings = new AnimationPhaseSettings();
        settings.type = type;
        settings.durationTicks = ticks;
        settings.properties = new AnimationPhaseProperties();
        settings.properties.color = color;
        return settings;
    }

    private static AnimationSettings copy(AnimationSettings source) {
        if (source == null) {
            return null;
        }
        AnimationSettings copy = new AnimationSettings();
        copy.preset = source.preset;
        copy.first = copyPhase(source.first);
        copy.second = copyPhase(source.second);
        copy.third = copyPhase(source.third);
        return copy;
    }

    private static AnimationPhaseSettings copyPhase(AnimationPhaseSettings source) {
        AnimationPhaseSettings copy = new AnimationPhaseSettings();
        copy.type = source.type;
        copy.durationTicks = source.durationTicks;
        copy.properties = new AnimationPhaseProperties();
        copy.properties.color = source.properties.color;
        copy.properties.useKeyAsModel = source.properties.useKeyAsModel;
        copy.properties.alignment = source.properties.alignment;
        copy.properties.offsetX = source.properties.offsetX;
        copy.properties.offsetY = source.properties.offsetY;
        copy.properties.offsetZ = source.properties.offsetZ;
        return copy;
    }
}
