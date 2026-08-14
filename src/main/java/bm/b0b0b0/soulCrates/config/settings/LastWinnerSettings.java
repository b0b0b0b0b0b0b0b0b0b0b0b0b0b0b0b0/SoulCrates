package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class LastWinnerSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Track global recent winners per crate for holograms and PlaceholderAPI."),
    })
    public boolean enabled = true;

    @Comment({
            @CommentValue("How many recent winners to keep per crate (max 20)."),
    })
    public int historySize = 20;
}
