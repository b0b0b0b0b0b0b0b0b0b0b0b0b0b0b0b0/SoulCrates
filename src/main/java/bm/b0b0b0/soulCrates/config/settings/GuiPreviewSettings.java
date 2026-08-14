package bm.b0b0b0.soulCrates.config.settings;

import java.util.List;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class GuiPreviewSettings extends YamlSerializable {

    public int size = 54;

    @Comment({
            @CommentValue("Slots where reward preview icons are drawn."),
    })
    public List<Integer> rewardSlots = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    );

    public int openSlot = 49;
    public int backSlot = 45;

    public boolean multiOpenButtons = true;

    public List<Integer> multiOpenSlots = List.of(47, 48, 51);

    public int multiOpenSlot5 = 47;

    public int multiOpenSlot10 = 51;

    public String fillerMaterial = "BLACK_STAINED_GLASS_PANE";
}
