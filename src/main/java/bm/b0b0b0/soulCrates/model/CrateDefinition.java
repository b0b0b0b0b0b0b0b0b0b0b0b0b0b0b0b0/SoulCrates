package bm.b0b0b0.soulCrates.model;

import bm.b0b0b0.soulCrates.config.settings.AnimationSettings;
import bm.b0b0b0.soulCrates.config.settings.KeySettings;
import bm.b0b0b0.soulCrates.config.settings.OpeningSettings;
import bm.b0b0b0.soulCrates.config.settings.PitySettings;
import bm.b0b0b0.soulCrates.config.settings.RerollSettings;
import java.util.List;
import org.bukkit.Material;

public record CrateDefinition(
        String id,
        String displayName,
        DisplayEngineKind engineKind,
        Material blockMaterial,
        String modelId,
        String idleAnimation,
        String closeAnimation,
        AnimationSettings animations,
        OpeningSettings opening,
        KeySettings keys,
        RerollSettings reroll,
        PitySettings pity,
        List<RewardDefinition> rewards
) {
}
