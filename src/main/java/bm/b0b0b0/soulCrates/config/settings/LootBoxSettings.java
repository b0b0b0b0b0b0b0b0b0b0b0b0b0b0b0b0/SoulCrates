package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class LootBoxSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Physical lootbox item that opens this crate from inventory."),
    })
    public boolean enabled = false;

    public String material = "CHEST";

    public int customModelData = -1;

    @Comment({
            @CommentValue("Show this crate on the lootbox-filtered preview page."),
    })
    public boolean showInPreview = true;

    @Comment({
            @CommentValue("Guaranteed rarity id for this lootbox open. Empty = normal roll."),
    })
    public String guaranteedRarity = "";
}
