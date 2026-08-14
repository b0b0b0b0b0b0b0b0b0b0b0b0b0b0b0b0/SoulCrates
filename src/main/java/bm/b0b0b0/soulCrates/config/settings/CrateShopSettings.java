package bm.b0b0b0.soulCrates.config.settings;

import java.util.ArrayList;
import java.util.List;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class CrateShopSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Virtual key shop entries."),
    })
    public boolean enabled = true;

    public List<ShopEntrySettings> entries = defaultEntries();

    private static List<ShopEntrySettings> defaultEntries() {
        List<ShopEntrySettings> entries = new ArrayList<>();
        ShopEntrySettings single = new ShopEntrySettings();
        single.crateId = "default";
        single.keyAmount = 1;
        single.vaultPrice = 500.0;
        ShopEntrySettings bundle = new ShopEntrySettings();
        bundle.crateId = "default";
        bundle.keyAmount = 5;
        bundle.vaultPrice = 2000.0;
        bundle.displayMaterial = "TRIPWIRE_HOOK";
        entries.add(single);
        entries.add(bundle);
        return entries;
    }
}
