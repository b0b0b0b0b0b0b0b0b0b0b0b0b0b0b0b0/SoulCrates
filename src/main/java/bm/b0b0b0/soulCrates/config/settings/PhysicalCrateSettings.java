package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class PhysicalCrateSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Physical placeable crates with unique instance id (anti-dupe)."),
    })
    public boolean enabled = true;

    @Comment({
            @CommentValue("Default item material when crate type has no override."),
    })
    public String material = "CHEST";

    @Comment({
            @CommentValue("Custom model data for physical crate item. -1 = disabled."),
    })
    public int customModelData = -1;

    @Comment({
            @CommentValue("Only the owner who received/placed the crate may open it."),
    })
    public boolean ownerOnlyOpen = true;

    @Comment({
            @CommentValue("Only the owner may break a placed crate to pick it up."),
    })
    public boolean ownerOnlyBreak = true;

    @Comment({
            @CommentValue("Return the physical crate item when owner breaks the block."),
    })
    public boolean returnItemOnBreak = true;
}
