package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class AnimationSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Preset replaces first/second/third below when set. Leave empty to tune phases manually."),
            @CommentValue("SHOWCASE — world BlockDisplay ring (vertical), crack intro, firework."),
            @CommentValue("CLASSIC — particle swirl only, no GUI and no block ring."),
            @CommentValue("BLAZING — flame column / lava burst, no ring."),
            @CommentValue("KEYSTORM — key VFX, horizontal block ring, helix reveal."),
            @CommentValue("CSGO_STYLE — inventory GUI spinner (csgo_gui), not world ring."),
            @CommentValue("FIREWORKS — bouncing dust ball, long firework finale."),
            @CommentValue("ARCADE — rising helix spiral particles, firework."),
            @CommentValue("BOUNCY — bouncing particle ball in world, no ring."),
            @CommentValue("SHULKER_PICK — shulker boxes on ground around player, hop pick, reveal losses."),
            @CommentValue("NONE — instant open."),
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
