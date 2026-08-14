package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class RerollSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Allow rerolling the reward after animation, before claim."),
    })
    public boolean enabled = false;

    @Comment({
            @CommentValue("Free rerolls per opening."),
    })
    public int freeRolls = 1;

    @Comment({
            @CommentValue("Maximum rerolls per opening including free rolls."),
    })
    public int maxRolls = 3;

    @Comment({
            @CommentValue("Vault cost per paid reroll after free rolls. 0 = free."),
    })
    public double vaultCost = 0.0;
}
