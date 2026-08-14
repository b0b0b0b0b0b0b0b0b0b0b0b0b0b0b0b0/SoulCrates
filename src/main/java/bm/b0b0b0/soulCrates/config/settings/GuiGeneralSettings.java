package bm.b0b0b0.soulCrates.config.settings;

import java.util.List;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class GuiGeneralSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Default GUI size (rows × 9). Must be a multiple of 9."),
    })
    public int size = 54;

    @Comment({
            @CommentValue("Debounce between GUI clicks in milliseconds (anti double-click)."),
    })
    public long clickDebounceMillis = 50L;

    @Comment({
            @CommentValue("Decorative border slots."),
    })
    public List<Integer> borderSlots = List.of(
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    );

    @NewLine
    @Comment({
            @CommentValue("Default filler material for border slots."),
    })
    public String fillerMaterial = "BLACK_STAINED_GLASS_PANE";

    public int fillerCustomModelData = -1;
}
