package bm.b0b0b0.soulCrates.config.settings;

import java.util.ArrayList;
import java.util.List;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class CrateShopSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Optional in-game virtual key shop (/sc shop). Off by default — most networks sell keys on a website."),
            @CommentValue("Requires Vault only when vault-price > 0. Crate must have virtual-keys enabled."),
    })
    public boolean enabled = false;

    public List<ShopEntrySettings> entries = new ArrayList<>();
}
