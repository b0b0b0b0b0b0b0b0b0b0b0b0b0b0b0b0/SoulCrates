package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class ShopEntrySettings extends YamlSerializable {

    public boolean enabled = true;

    public String crateId = "default";

    public int keyAmount = 1;

    @Comment({
            @CommentValue("Vault price. 0 = free (still checks item cost if set)."),
    })
    public double vaultPrice = 1000.0;

    @Comment({
            @CommentValue("Item cost MATERIAL:amount. Empty = no item cost."),
    })
    public String itemCost = "";

    public String displayMaterial = "TRIPWIRE_HOOK";
}
