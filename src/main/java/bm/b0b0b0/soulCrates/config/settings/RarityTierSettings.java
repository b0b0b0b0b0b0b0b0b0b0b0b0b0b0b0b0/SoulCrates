package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class RarityTierSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Unique rarity id referenced by rewards and keys."),
    })
    public String id = "common";

    public String displayName = "Common";

    @Comment({
            @CommentValue("Roll weight for this rarity tier."),
    })
    public double weight = 70.0;

    @Comment({
            @CommentValue("MiniMessage color/prefix shown in preview lore."),
    })
    public String color = "<gray>";
}
