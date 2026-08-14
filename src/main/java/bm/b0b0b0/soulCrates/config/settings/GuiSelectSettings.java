package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class GuiSelectSettings extends YamlSerializable {

    public int size = 54;

    @NewLine
    public GuiRewardGridSettings grid = new GuiRewardGridSettings();

    public int keysInfoSlot = 48;

    @Comment({
            @CommentValue("Close button slot. -1 = disabled (use ESC)."),
    })
    public int backSlot = -1;

    public String backMaterial = "BARRIER";

    public String lockedMaterial = "RED_STAINED_GLASS_PANE";
}
