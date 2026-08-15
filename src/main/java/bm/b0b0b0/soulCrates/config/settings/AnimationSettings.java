package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class AnimationSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Preset replaces first/second/third below when set. Leave empty to tune phases manually."),
            @CommentValue("SHOWCASE — world BlockDisplay ring (vertical), crack intro, firework."),
            @CommentValue("CSGO_STYLE — inventory GUI spinner (csgo_gui), not world ring."),
            @CommentValue("SHULKER_PICK — shulker boxes on ground around player, hop pick, reveal losses."),
            @CommentValue("MOB_PICK — mobs in a circle, hit to choose, reveal others."),
    })
    public String preset = "SHOWCASE";

    @NewLine
    @Comment({@CommentValue("Phase 1 — pre-open / key insert.")})
    public AnimationPhaseSettings first = defaultPhase("default", 40);

    @Comment({@CommentValue("Phase 2 — carousel / spinner / swirl.")})
    public AnimationPhaseSettings second = defaultPhase("carousel", 220);

    @Comment({@CommentValue("Phase 3 — firework reveal or none.")})
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
