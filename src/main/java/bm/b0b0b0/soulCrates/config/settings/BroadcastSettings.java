package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class BroadcastSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Global chat broadcast for rewards with broadcast: true."),
    })
    public boolean enabled = true;
}
