package bm.b0b0b0.soulCrates.config.settings;

import java.util.List;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class HologramSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Floating labels above bound crate blocks."),
    })
    public boolean enabled = true;

    public double offsetY = 2.1;

    @Comment({
            @CommentValue("VANILLA (TextDisplay), DECENT_HOLOGRAMS, or FANCY_HOLOGRAMS"),
    })
    public String provider = "VANILLA";

    public List<String> lines = List.of(
            "<gold>{crate}</gold>",
            "<gray>Click to open · Shift preview</gray>"
    );
}
