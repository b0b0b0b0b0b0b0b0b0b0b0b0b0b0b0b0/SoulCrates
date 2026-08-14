package bm.b0b0b0.soulCrates.animation;

import bm.b0b0b0.soulCrates.config.settings.AnimationPhaseSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiSpinnerSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import org.bukkit.plugin.java.JavaPlugin;

public final class PhaseFactory {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final GuiSpinnerSettings spinnerSettings;

    public PhaseFactory(JavaPlugin plugin, MessageService messageService, GuiSpinnerSettings spinnerSettings) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.spinnerSettings = spinnerSettings;
    }

    public PhaseRunner create(CrateDefinition crateDefinition, OpeningPhaseKind kind, RewardDefinition rolledReward) {
        AnimationPhaseSettings settings = switch (kind) {
            case FIRST -> crateDefinition.animations().first;
            case SECOND -> crateDefinition.animations().second;
            case THIRD -> crateDefinition.animations().third;
        };
        String type = settings.type == null ? "default" : settings.type.toLowerCase();
        return switch (kind) {
            case FIRST -> new KeyInsertPhase(type, settings.durationTicks);
            case SECOND -> createSecondPhase(crateDefinition, settings, type, rolledReward);
            case THIRD -> createThirdPhase(settings, type, rolledReward);
        };
    }

    private PhaseRunner createSecondPhase(
            CrateDefinition crateDefinition,
            AnimationPhaseSettings settings,
            String type,
            RewardDefinition rolledReward
    ) {
        if ("csgo".equals(type) || "spinner".equals(type) || "carousel".equals(type)) {
            return new CsgoSpinnerPhase(
                    plugin,
                    messageService,
                    spinnerSettings,
                    crateDefinition,
                    rolledReward,
                    settings.durationTicks
            );
        }
        return new SpinnerPhase(type, settings.durationTicks);
    }

    private PhaseRunner createThirdPhase(AnimationPhaseSettings settings, String type, RewardDefinition rolledReward) {
        if ("firework".equals(type) || "reveal".equals(type)) {
            return new FireworkRevealPhase(rolledReward, settings.durationTicks);
        }
        return new RewardRevealPhase(type, settings.durationTicks);
    }
}
