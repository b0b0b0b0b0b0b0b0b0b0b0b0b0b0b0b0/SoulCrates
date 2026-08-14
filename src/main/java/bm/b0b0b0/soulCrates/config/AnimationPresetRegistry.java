package bm.b0b0b0.soulCrates.config;

import bm.b0b0b0.soulCrates.config.settings.AnimationPhaseProperties;
import bm.b0b0b0.soulCrates.config.settings.AnimationPhaseSettings;
import bm.b0b0b0.soulCrates.config.settings.AnimationSettings;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class AnimationPresetRegistry {

    private static final Map<String, AnimationSettings> PRESETS = new ConcurrentHashMap<>();

    static {
        register("SHOWCASE", phase("crack", 25, "#22d3ee"), phase("carousel", 220, "#55ff55", "VERTICAL"), phase("firework", 35, "#fbbf24"));
        register("CLASSIC", phase("default", 35, "#e5e5e5"), phase("swirl", 95, "#9ca3af"), phase("default", 40, "#d4d4d4"));
        register("BLAZING", phase("fire", 25, "#ff5500"), phase("fire", 110, "#ff6600"), phase("firework", 50, "#ff3300"));
        register("KEYSTORM", phase("key_opener", 45, "#ffffff"), phase("carousel", 160, "#a78bfa", "HORIZONTAL"), phase("helix", 55, "#c084fc"));
        register("CSGO_STYLE", phase("crack", 20, "#55ff55"), phase("csgo_gui", 200, "#55ff55"), phase("firework", 45, "#fbbf24"));
        register("FIREWORKS", phase("default", 15, "#ffffff"), phase("ball", 85, "#f59e0b"), phase("firework", 65, "#ef4444"));
        register("ARCADE", phase("crack", 25, "#22d3ee"), phase("helix", 130, "#a855f7"), phase("firework", 50, "#34d399"));
        register("BOUNCY", phase("default", 20, "#ffffff"), phase("ball", 95, "#34d399"), phase("ball", 40, "#22c55e"));
        register("SHULKER_PICK", phase("default", 18, "#ffffff"), phase("shulker_pick", 240, "#a78bfa"), phase("none", 12, "#c084fc"));
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

    public static boolean isKnownPreset(String presetId) {
        if (presetId == null || presetId.isBlank()) {
            return false;
        }
        return PRESETS.containsKey(presetId.trim().toUpperCase(Locale.ROOT));
    }

    public static List<String> presetIds() {
        List<String> ids = new ArrayList<>(PRESETS.keySet());
        ids.sort(String::compareTo);
        return List.copyOf(ids);
    }

    public static void applyPreset(AnimationSettings target, String presetId) {
        if (target == null || presetId == null || presetId.isBlank()) {
            return;
        }
        String normalized = presetId.trim().toUpperCase(Locale.ROOT);
        AnimationSettings preset = resolve(normalized);
        if (preset == null) {
            return;
        }
        target.preset = normalized;
        target.first = copyPhase(preset.first);
        target.second = copyPhase(preset.second);
        target.third = copyPhase(preset.third);
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
        return phase(type, ticks, color, null);
    }

    private static AnimationPhaseSettings phase(String type, int ticks, String color, String alignment) {
        AnimationPhaseSettings settings = new AnimationPhaseSettings();
        settings.type = type;
        settings.durationTicks = ticks;
        settings.properties = new AnimationPhaseProperties();
        settings.properties.color = color;
        if (alignment != null && !alignment.isBlank()) {
            settings.properties.alignment = alignment;
        }
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
        copy.properties.suspenseEnabled = source.properties.suspenseEnabled;
        copy.properties.suspenseMoments = source.properties.suspenseMoments;
        return copy;
    }
}
