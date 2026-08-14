package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class AnimationSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Phoenix-style preset: CLASSIC, BLAZING, KEYSTORM, CSGO_STYLE, FIREWORKS, ARCADE, BOUNCY, NONE."),
            @CommentValue("When set, replaces first/second/third unless you leave preset empty."),
    })
    public String preset = "CLASSIC";

    @NewLine
    @Comment({@CommentValue("Phase 1 — pre-open / key insert.")})
    public AnimationPhaseSettings first = defaultPhase("default", 40);

    @Comment({@CommentValue("Phase 2 — spinner / swirl / CSGO.")})
    public AnimationPhaseSettings second = defaultPhase("swirl", 60);

    @Comment({@CommentValue("Phase 3 — reveal / helix / firework.")})
    public AnimationPhaseSettings third = defaultPhase("default", 40);

    private static AnimationPhaseSettings defaultPhase(String type, int ticks) {
        AnimationPhaseSettings settings = new AnimationPhaseSettings();
        settings.type = type;
        settings.durationTicks = ticks;
        settings.properties = new AnimationPhaseProperties();
        settings.properties.color = "#404040";
        return settings;
    }
}
