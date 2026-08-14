package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class AnimationSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Preset replaces first/second/third below when set. Leave empty to tune phases manually."),
            @CommentValue("SHOWCASE — crack intro, world BlockDisplay roulette (recommended), firework reveal."),
            @CommentValue("CLASSIC — soft particles, swirl spin, calm finish."),
            @CommentValue("BLAZING — fire blast, fast orange roulette, firework."),
            @CommentValue("KEYSTORM — key insert VFX, purple roulette, helix reveal."),
            @CommentValue("CSGO_STYLE — fire intro, green roulette (world ring, not GUI), firework."),
            @CommentValue("FIREWORKS — short intro, warm roulette, red firework finale."),
            @CommentValue("ARCADE — cyan crack, purple roulette, green firework."),
            @CommentValue("BOUNCY — light intro, bouncing ball spin (particles), soft finish."),
            @CommentValue("NONE — instant open, no animation."),
    })
    public String preset = "SHOWCASE";

    @NewLine
    @Comment({@CommentValue("Phase 1 — pre-open / key insert.")})
    public AnimationPhaseSettings first = defaultPhase("default", 40);

    @Comment({@CommentValue("Phase 2 — carousel / spinner / swirl.")})
    public AnimationPhaseSettings second = defaultPhase("carousel", 220);

    @Comment({@CommentValue("Phase 3 — reveal / helix / firework.")})
    public AnimationPhaseSettings third = defaultPhase("default", 40);

    private static AnimationPhaseSettings defaultPhase(String type, int ticks) {
        AnimationPhaseSettings settings = new AnimationPhaseSettings();
        settings.type = type;
        settings.durationTicks = ticks;
        settings.properties = new AnimationPhaseProperties();
        settings.properties.color = "#55ff55";
        settings.properties.alignment = "VERTICAL";
        return settings;
    }
}
