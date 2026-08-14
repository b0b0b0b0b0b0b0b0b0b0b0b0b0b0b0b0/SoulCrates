package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class KeySettings extends YamlSerializable {

    @Comment({
            @CommentValue("Enable key item + virtual key storage for this crate."),
    })
    public boolean enabled = false;

    @Comment({
            @CommentValue("Physical key item material."),
    })
    public String material = "TRIPWIRE_HOOK";

    public int customModelData = -1;

    @Comment({
            @CommentValue("Accept physical key items from player inventory."),
    })
    public boolean physicalKeys = true;

    @Comment({
            @CommentValue("Accept virtual keys stored in database."),
    })
    public boolean virtualKeys = true;

    @Comment({
            @CommentValue("Guaranteed rarity for keys of this crate. Empty = normal roll."),
    })
    public String guaranteedRarity = "";

    @Comment({
            @CommentValue("Key rarity tier for SELECT mode gating. Empty = any reward."),
    })
    public String rarity = "";
}
