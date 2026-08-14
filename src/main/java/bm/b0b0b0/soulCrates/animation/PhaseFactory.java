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
            case "key_opener", "key-opener", "key_insert", "key-insert" ->
                    new WorldParticlePhase(OpeningPhaseKind.FIRST, WorldParticlePhase.Style.KEY_OPENER, settings, rolledReward);
            case "crack", "lightning" ->
                    new WorldParticlePhase(OpeningPhaseKind.FIRST, WorldParticlePhase.Style.CRACK, settings, rolledReward);
            case "fire", "blast", "blasting" ->
                    new WorldParticlePhase(OpeningPhaseKind.FIRST, WorldParticlePhase.Style.FIRE, settings, rolledReward);
            case "swirl" ->
                    new WorldParticlePhase(OpeningPhaseKind.FIRST, WorldParticlePhase.Style.SWIRL, settings, rolledReward);
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
        return switch (type) {
            case "none" -> new WorldParticlePhase(OpeningPhaseKind.SECOND, WorldParticlePhase.Style.NONE, settings, rolledReward);
            case "swirl" -> new WorldParticlePhase(OpeningPhaseKind.SECOND, WorldParticlePhase.Style.SWIRL, settings, rolledReward);
            case "ball" -> new WorldParticlePhase(OpeningPhaseKind.SECOND, WorldParticlePhase.Style.BALL, settings, rolledReward);
            case "fire" -> new WorldParticlePhase(OpeningPhaseKind.SECOND, WorldParticlePhase.Style.FIRE, settings, rolledReward);
            case "crack" -> new WorldParticlePhase(OpeningPhaseKind.SECOND, WorldParticlePhase.Style.CRACK, settings, rolledReward);
            default -> new WorldParticlePhase(OpeningPhaseKind.SECOND, WorldParticlePhase.Style.SWIRL, settings, rolledReward);
        };
    }

    private PhaseRunner createThirdPhase(AnimationPhaseSettings settings, String type, RewardDefinition rolledReward) {
        if ("firework".equals(type) || "reveal".equals(type) || "fireworks".equals(type)) {
            return new FireworkRevealPhase(rolledReward, settings.durationTicks);
        }
        return switch (type) {
            case "none" -> new WorldParticlePhase(OpeningPhaseKind.THIRD, WorldParticlePhase.Style.NONE, settings, rolledReward);
            case "helix", "smoke_spiral", "fire_spiral" ->
                    new WorldParticlePhase(OpeningPhaseKind.THIRD, WorldParticlePhase.Style.HELIX, settings, rolledReward);
            case "ball" -> new WorldParticlePhase(OpeningPhaseKind.THIRD, WorldParticlePhase.Style.BALL, settings, rolledReward);
            case "swirl" -> new WorldParticlePhase(OpeningPhaseKind.THIRD, WorldParticlePhase.Style.SWIRL, settings, rolledReward);
            case "fire" -> new WorldParticlePhase(OpeningPhaseKind.THIRD, WorldParticlePhase.Style.FIRE, settings, rolledReward);
            default -> new WorldParticlePhase(OpeningPhaseKind.THIRD, WorldParticlePhase.Style.DEFAULT, settings, rolledReward);
        };
    }

    private static boolean isGuiSpinnerType(String type) {
        return "csgo_gui".equals(type)
                || "rainbow_gui".equals(type)
                || "flip_gui".equals(type)
                || "snake_gui".equals(type);
    }

    private static boolean isWorldCarouselType(String type) {
        return "csgo".equals(type)
                || "spinner".equals(type)
                || "carousel".equals(type)
                || "world_carousel".equals(type)
                || "roulette".equals(type)
                || "orbit".equals(type)
                || "ring".equals(type)
                || "display".equals(type);
    }

    private static boolean isMobPickType(String type) {
        return "mob_pick".equals(type)
                || "mob_circle".equals(type)
                || "kill_mob_circle".equals(type)
                || "mystery_mobs".equals(type)
                || "mob_roulette".equals(type);
    }

    private static boolean isShulkerPickType(String type) {
        return "shulker_pick".equals(type)
                || "shulker".equals(type)
                || "ground_pick".equals(type)
                || "mystery_shulkers".equals(type);
    }

    private static String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "default";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String mapModelEngineAnimation(String type) {
        return switch (type) {
            case "key_opener", "key_insert", "crack", "lightning", "fire" -> "pre-open";
            case "csgo", "csgo_gui", "spinner", "swirl", "ball", "carousel", "shulker_pick", "shulker", "ground_pick", "mystery_shulkers", "mob_pick", "mob_circle", "kill_mob_circle", "mystery_mobs", "mob_roulette" -> "open";
            case "firework", "helix", "reveal", "default" -> "display";
            case "none" -> "idle";
            default -> type;
        };
    }
}
