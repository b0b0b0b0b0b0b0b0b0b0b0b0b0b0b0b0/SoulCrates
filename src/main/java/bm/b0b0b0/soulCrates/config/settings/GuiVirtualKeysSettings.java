package bm.b0b0b0.soulCrates.config.settings;

import java.util.List;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class GuiVirtualKeysSettings extends YamlSerializable {

    public int size = 54;

    @Comment({
            @CommentValue("Slots listing crate keys."),
    })
    public List<Integer> entrySlots = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    );

    public int backSlot = 49;

    public String fillerMaterial = "BLACK_STAINED_GLASS_PANE";
}
