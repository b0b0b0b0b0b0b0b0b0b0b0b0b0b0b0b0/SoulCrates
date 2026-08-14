package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class IdleEffectSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Pattern: DEFAULT, CIRCLE, STAR, SQUARE, SPIRAL, PULSE."),
    })
    public String pattern = "DEFAULT";

    public String particle = "REDSTONE";

    public String color = "#ff0000";

    public double offsetX = 0.0;

    public double offsetY = 0.0;

    public double offsetZ = 0.0;

    public double spread = 1.0;

    public double velocity = 0.1;

    public int amount = 2;
}
