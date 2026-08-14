package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class AnimationDisplaySettings extends YamlSerializable {

    @Comment({
            @CommentValue("Horizontal shift of the roulette center from crate block center."),
    })
    public double rewardItemOffsetX = 0.0;

    @Comment({
            @CommentValue("Height of roulette center above crate block top (blocks)."),
            @CommentValue("Auto-clamped so ring bottom does not clip the crate. Default 1.5."),
    })
    public double rewardItemOffsetY = 1.5;

    @Comment({
            @CommentValue("Depth shift of roulette center from crate block center."),
    })
    public double rewardItemOffsetZ = 0.0;

    @Comment({
            @CommentValue("Extra shift for reward name TextDisplay above each block."),
    })
    public double rewardNameOffsetX = 0.0;

    public double rewardNameOffsetY = 0.3;

    public double rewardNameOffsetZ = 0.0;

    public boolean skipOnEsc = false;
}
