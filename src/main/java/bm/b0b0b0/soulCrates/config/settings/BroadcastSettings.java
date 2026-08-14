package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class BroadcastSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Global chat broadcast for crate wins (also shown in console)."),
    })
    public boolean enabled = true;

    @Comment({
            @CommentValue("Multi-line server broadcast when a crate win is revealed."),
    })
    public boolean dramaticMultiLine = true;

    @Comment({
            @CommentValue("Broadcast every open. If false, only rewards with broadcast: true in crate yml."),
    })
    public boolean broadcastAllOpens = false;
}
