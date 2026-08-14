package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class AnimationPhaseProperties extends YamlSerializable {

    public String color = "#ffffff";

    public boolean useKeyAsModel = false;

    public String alignment = "VERTICAL";

    public double offsetX = 0.0;

    public double offsetY = 0.0;

    public double offsetZ = 0.0;

    @Comment({
            @CommentValue("Carousel only: gentle fake pauses and second-wind surges while spinning."),
    })
    public boolean suspenseEnabled = true;

    @Comment({
            @CommentValue("Carousel only: suspense beats per open (1-3). Seeded, not chaotic."),
    })
    public int suspenseMoments = 2;
}
