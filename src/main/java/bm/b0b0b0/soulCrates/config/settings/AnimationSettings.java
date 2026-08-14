package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class AnimationSettings extends YamlSerializable {

    @NewLine
    @Comment({@CommentValue("Phase 1 — key insert / charge.")})
    public AnimationPhaseSettings first = defaultPhase("key_insert", 20);

    @Comment({@CommentValue("Phase 2 — spinner / CSGO carousel.")})
    public AnimationPhaseSettings second = defaultPhase("csgo", 80);

    @Comment({@CommentValue("Phase 3 — reveal / firework.")})
    public AnimationPhaseSettings third = defaultPhase("firework", 40);

    private static AnimationPhaseSettings defaultPhase(String type, int ticks) {
        AnimationPhaseSettings settings = new AnimationPhaseSettings();
        settings.type = type;
        settings.durationTicks = ticks;
        return settings;
    }
}
