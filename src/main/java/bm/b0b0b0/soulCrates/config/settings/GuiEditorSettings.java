package bm.b0b0b0.soulCrates.config.settings;

import java.util.List;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class GuiEditorSettings extends YamlSerializable {

    public int size = 54;

    @NewLine
    public GuiRewardGridSettings grid = new GuiRewardGridSettings();

    public List<Integer> crateSlots = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    );

    public int reloadSlot = 49;

    @Comment({
            @CommentValue("Add-reward button in reward list."),
    })
    public int addRewardSlot = 49;

    public String addRewardMaterial = "WRITABLE_BOOK";

    public int backSlot = 53;

    public String backMaterial = "BARRIER";
}
