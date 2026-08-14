package bm.b0b0b0.soulCrates.config.settings;

import java.util.List;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class GuiPreviewSettings extends YamlSerializable {

    public int size = 54;

    @NewLine
    public GuiRewardGridSettings grid = new GuiRewardGridSettings();

    @Comment({
            @CommentValue("Primary open button. Slot 49 by default."),
    })
    public int openSlot = 49;

    public String openMaterial = "CHEST";

    public boolean multiOpenButtons = true;

    public List<Integer> multiOpenSlots = List.of(47, 48, 51);

    public List<String> multiOpenMaterials = List.of("IRON_BLOCK", "GOLD_BLOCK", "DIAMOND_BLOCK");

    @Comment({
            @CommentValue("Close / back button slot. -1 = disabled (use ESC)."),
    })
    public int backSlot = -1;

    public String backMaterial = "BARRIER";
}
