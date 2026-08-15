package bm.b0b0b0.soulCrates.animation;

import bm.b0b0b0.soulCrates.config.settings.AnimationPhaseSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiSpinnerSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.service.reward.BroadcastService;
import java.util.Locale;
import org.bukkit.plugin.java.JavaPlugin;

public final class PhaseFactory {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final BroadcastService broadcastService;
    private final GuiSpinnerSettings spinnerSettings;

    public PhaseFactory(
            JavaPlugin plugin,
            MessageService messageService,
            BroadcastService broadcastService,
            GuiSpinnerSettings spinnerSettings
    ) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.broadcastService = broadcastService;
        this.spinnerSettings = spinnerSettings;
    }

    public PhaseRunner create(CrateDefinition crateDefinition, OpeningPhaseKind kind, RewardDefinition rolledReward) {
        AnimationPhaseSettings settings = switch (kind) {
            case FIRST -> crateDefinition.animations().first;
            case SECOND -> crateDefinition.animations().second;
            case THIRD -> crateDefinition.animations().third;
        };
        String type = normalizeType(settings.type);
        return switch (kind) {
            case FIRST -> createFirstPhase(settings, type, rolledReward);
            case SECOND -> createSecondPhase(crateDefinition, settings, type, rolledReward);
            case THIRD -> createThirdPhase(settings, type, rolledReward);
        };
    }

    public boolean usesCsgoSpinner(CrateDefinition crateDefinition) {
        return isGuiSpinnerType(normalizeType(crateDefinition.animations().second.type));
    }

    public String animationId(CrateDefinition crateDefinition, OpeningPhaseKind kind) {
        AnimationPhaseSettings settings = switch (kind) {
            case FIRST -> crateDefinition.animations().first;
            case SECOND -> crateDefinition.animations().second;
            case THIRD -> crateDefinition.animations().third;
        };
        return mapModelEngineAnimation(normalizeType(settings.type));
    }

    private PhaseRunner createFirstPhase(AnimationPhaseSettings settings, String type, RewardDefinition rolledReward) {
        return switch (type) {
            case "none" -> new WorldParticlePhase(OpeningPhaseKind.FIRST, WorldParticlePhase.Style.NONE, settings, rolledReward);
            case "crack", "lightning" ->
                    new WorldParticlePhase(OpeningPhaseKind.FIRST, WorldParticlePhase.Style.CRACK, settings, rolledReward);
            default ->
                    new WorldParticlePhase(OpeningPhaseKind.FIRST, WorldParticlePhase.Style.DEFAULT, settings, rolledReward);
        };
    }

    private PhaseRunner createSecondPhase(
            CrateDefinition crateDefinition,
            AnimationPhaseSettings settings,
            String type,
            RewardDefinition rolledReward
    ) {
        if (isGuiSpinnerType(type)) {
            return new CsgoSpinnerPhase(
                    plugin,
                    messageService,
                    spinnerSettings,
                    crateDefinition,
                    rolledReward,
                    settings.durationTicks
            );
        }
        if (isWorldCarouselType(type)) {
            return new WorldCarouselPhase(
                    messageService,
                    broadcastService,
                    crateDefinition,
                    rolledReward,
                    settings
            );
        }
        if (isShulkerPickType(type)) {
            return new ShulkerPickPhase(
                    messageService,
                    broadcastService,
                    crateDefinition,
                    rolledReward,
                    settings
            );
        }
        if (isMobPickType(type)) {
            return new MobCirclePickPhase(
                    plugin,
                    messageService,
                    broadcastService,
                    crateDefinition,
                    rolledReward,
                    settings
            );
        }
        return new WorldParticlePhase(OpeningPhaseKind.SECOND, WorldParticlePhase.Style.NONE, settings, rolledReward);
    }

    private PhaseRunner createThirdPhase(AnimationPhaseSettings settings, String type, RewardDefinition rolledReward) {
        if ("firework".equals(type) || "reveal".equals(type) || "fireworks".equals(type)) {
            return new FireworkRevealPhase(rolledReward, settings.durationTicks);
        }
        return switch (type) {
            case "none" -> new WorldParticlePhase(OpeningPhaseKind.THIRD, WorldParticlePhase.Style.NONE, settings, rolledReward);
            default -> new WorldParticlePhase(OpeningPhaseKind.THIRD, WorldParticlePhase.Style.NONE, settings, rolledReward);
        };
    }

    private static boolean isGuiSpinnerType(String type) {
        return "csgo_gui".equals(type) || "csgo".equals(type);
    }

    private static boolean isWorldCarouselType(String type) {
        return "carousel".equals(type) || "world_carousel".equals(type);
    }

    private static boolean isMobPickType(String type) {
        return "mob_pick".equals(type);
    }

    private static boolean isShulkerPickType(String type) {
        return "shulker_pick".equals(type);
    }

    private static String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "default";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String mapModelEngineAnimation(String type) {
        return switch (type) {
            case "crack", "lightning" -> "pre-open";
            case "csgo", "csgo_gui", "carousel", "world_carousel", "shulker_pick", "mob_pick" -> "open";
            case "firework", "reveal", "default" -> "display";
            case "none" -> "idle";
            default -> type;
        };
    }
}
