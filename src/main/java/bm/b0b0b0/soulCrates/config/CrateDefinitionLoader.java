package bm.b0b0b0.soulCrates.config;

import bm.b0b0b0.soulCrates.config.settings.CrateDefinitionSettings;
import bm.b0b0b0.soulCrates.config.settings.RarityTierSettings;
import bm.b0b0b0.soulCrates.config.settings.RewardEntrySettings;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.DisplayEngineKind;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;

public final class CrateDefinitionLoader {

    private CrateDefinitionLoader() {
    }

    public static List<CrateDefinition> loadDirectory(Path cratesDirectory) {
        try {
            Files.createDirectories(cratesDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create crates directory", exception);
        }
        Path defaultFile = cratesDirectory.resolve("default.yml");
        if (!Files.exists(defaultFile)) {
            SerializedConfigReloader.reload(new CrateDefinitionSettings(), defaultFile);
        }
        List<CrateDefinition> crates = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cratesDirectory, "*.yml")) {
            for (Path file : stream) {
                CrateDefinitionSettings settings = SerializedConfigReloader.reload(new CrateDefinitionSettings(), file);
                if (settings.id == null || settings.id.isBlank()) {
                    String fileName = file.getFileName().toString();
                    settings.id = fileName.substring(0, fileName.length() - 4).toLowerCase(Locale.ROOT);
                }
                crates.add(toDefinition(settings));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load crate definitions", exception);
        }
        return crates;
    }

    public static CrateDefinition toDefinition(CrateDefinitionSettings settings) {
        DisplayEngineKind engineKind = parseEngineKind(settings.engine.type);
        Material material = Material.matchMaterial(settings.engine.blockMaterial);
        if (material == null || material.isAir()) {
            material = Material.ENDER_CHEST;
        }
        List<RewardDefinition> rewards = new ArrayList<>();
        for (RewardEntrySettings entry : settings.rewards) {
            rewards.add(new RewardDefinition(
                    entry.id.toLowerCase(Locale.ROOT),
                    entry.rarity == null ? "" : entry.rarity.toLowerCase(Locale.ROOT),
                    Math.max(0.0, entry.weight),
                    entry.displayName,
                    entry.material,
                    entry.customModelData,
                    List.copyOf(entry.grants),
                    List.copyOf(entry.commands),
                    entry.pityEligible,
                    entry.broadcast
            ));
        }
        List<RarityTierSettings> rarities = settings.rarities == null ? List.of() : List.copyOf(settings.rarities);
        return new CrateDefinition(
                settings.id.toLowerCase(Locale.ROOT),
                settings.displayName,
                engineKind,
                material,
                settings.engine.modelId == null ? "" : settings.engine.modelId,
                settings.engine.idleAnimation,
                settings.engine.closeAnimation,
                settings.animations,
                settings.opening,
                settings.keys,
                settings.reroll,
                settings.pity,
                settings.lootBox,
                rarities,
                List.copyOf(rewards)
        );
    }

    private static DisplayEngineKind parseEngineKind(String raw) {
        if (raw == null) {
            return DisplayEngineKind.VANILLA_DISPLAY;
        }
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "VANILLA_BLOCK", "BLOCK" -> DisplayEngineKind.VANILLA_BLOCK;
            case "MODEL_ENGINE", "MODELENGINE" -> DisplayEngineKind.MODEL_ENGINE;
            default -> DisplayEngineKind.VANILLA_DISPLAY;
        };
    }
}
