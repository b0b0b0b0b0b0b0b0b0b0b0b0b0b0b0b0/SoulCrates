package bm.b0b0b0.soulCrates.config.settings;

import java.util.List;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class MassOpeningSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Enable mass-open buttons and /sc open <crate> <amount> for this crate."),
    })
    public boolean enabled = true;

    @Comment({
            @CommentValue("Preset amounts shown in preview GUI (use -1 slot in gui for open-all-keys)."),
    })
    public List<Integer> presets = List.of(5, 10, -1);

    public int maxAmount = 10;

    @Comment({
            @CommentValue("Allow opening with all available keys at once."),
    })
    public boolean allowOpenAll = true;
}
