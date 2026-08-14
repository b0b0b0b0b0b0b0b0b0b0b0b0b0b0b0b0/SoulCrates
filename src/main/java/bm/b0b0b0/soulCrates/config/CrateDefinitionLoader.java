package bm.b0b0b0.soulCrates.config;

import bm.b0b0b0.soulCrates.config.AnimationPresetRegistry;
import bm.b0b0b0.soulCrates.config.settings.CrateDefinitionSettings;
import bm.b0b0b0.soulCrates.config.settings.IdleEffectSettings;
import bm.b0b0b0.soulCrates.config.settings.RarityTierSettings;
import bm.b0b0b0.soulCrates.config.settings.RewardEntrySettings;
import bm.b0b0b0.soulCrates.config.settings.AnimationDisplaySettings;
import bm.b0b0b0.soulCrates.config.settings.AnimationSettings;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.DisplayEngineKind;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.model.RewardWinLimits;
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
            CrateDefinitionSettings defaults = new CrateDefinitionSettings();
            defaults.save(defaultFile);
            CrateYamlPresenter.polish(defaultFile);
        }
        ensureExampleCrates(cratesDirectory);
        List<CrateDefinition> crates = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cratesDirectory, "*.yml")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                if (fileName.startsWith("_")) {
                    continue;
                }
                CrateYamlPresenter.polish(file);
                CrateDefinitionSettings settings = CrateYamlLoadGuard.reloadCrateSettings(new CrateDefinitionSettings(), file);
                if (settings.id == null || settings.id.isBlank()) {
                    settings.id = fileName.substring(0, fileName.length() - 4).toLowerCase(Locale.ROOT);
                }
                crates.add(toDefinition(settings));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load crate definitions", exception);
        }
        return crates;
    }

    private static void ensureExampleCrates(Path cratesDirectory) {
        ensureExampleCrate(cratesDirectory, "blazing", "Blazing Crate", "BLAZING", "#ff5500");
        ensureExampleCrate(cratesDirectory, "arcade", "Arcade Crate", "ARCADE", "#a855f7");
        ensureExampleCrate(cratesDirectory, "keystorm", "Keystorm Crate", "KEYSTORM", "#a78bfa");
        ensureExampleCrate(cratesDirectory, "classic", "Classic Crate", "CLASSIC", "#cccccc");
    }

    private static void ensureExampleCrate(
            Path cratesDirectory,
            String id,
            String displayName,
            String preset,
            String idleColor
    ) {
        Path file = cratesDirectory.resolve(id + ".yml");
        if (Files.exists(file)) {
            return;
        }
        CrateDefinitionSettings settings = new CrateDefinitionSettings();
        settings.id = id;
        settings.displayName = displayName;
        AnimationPresetRegistry.applyPreset(settings.animations, preset);
        if (!settings.idleEffects.isEmpty()) {
            settings.idleEffects.get(0).color = idleColor;
        }
        settings.save(file);
        CrateYamlPresenter.polish(file);
    }

    public static CrateDefinition toDefinition(CrateDefinitionSettings settings) {
        DisplayEngineKind engineKind = parseEngineKind(settings.engine.type);
        Material material = Material.matchMaterial(settings.engine.blockMaterial);
        if (material == null || material.isAir()) {
            material = Material.ENDER_CHEST;
        }
        List<RewardDefinition> rewards = new ArrayList<>();
        for (RewardEntrySettings entry : settings.rewards) {
            if (!entry.enabled) {
                continue;
            }
            RewardWinLimits limits = new RewardWinLimits(
                    entry.playerWinLimit,
                    entry.globalWinLimit,
                    entry.winLimitCooldownSeconds,
                    entry.globalWinLimitCooldownSeconds,
                    entry.expiresAtEpochMs,
                    entry.requiredKeys,
                    List.copyOf(entry.requiredPermissions),
                    List.copyOf(entry.restrictedPermissions),
                    entry.alternative
            );
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
                    entry.broadcast,
                    true,
                    limits
            ));
        }
        List<RarityTierSettings> rarities = settings.rarities == null ? List.of() : List.copyOf(settings.rarities);
        List<IdleEffectSettings> idleEffects = settings.idleEffects == null ? List.of() : List.copyOf(settings.idleEffects);
        AnimationSettings animations = AnimationPresetRegistry.resolveOrDefault(settings.animations);
        AnimationDisplaySettings animationDisplay = settings.animationDisplay == null
                ? new AnimationDisplaySettings()
                : settings.animationDisplay;
        return new CrateDefinition(
                settings.id.toLowerCase(Locale.ROOT),
                settings.displayName,
                engineKind,
                material,
                settings.engine.modelId == null ? "" : settings.engine.modelId,
                settings.engine.idleAnimation,
                settings.engine.closeAnimation,
                animations,
                animationDisplay,
                idleEffects,
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
