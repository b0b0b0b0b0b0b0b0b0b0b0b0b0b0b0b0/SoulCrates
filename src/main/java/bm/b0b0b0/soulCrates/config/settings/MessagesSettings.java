package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class MessagesSettings extends YamlSerializable {

    @Comment({
            @CommentValue("CLIENT — each player uses lang/messages_<code>.yml from Minecraft language (default)."),
            @CommentValue("SERVER — everyone uses server-locale below."),
    })
    public String localeMode = "CLIENT";

    @Comment({
            @CommentValue("Used when locale-mode is SERVER. File: lang/messages_<code>.yml"),
    })
    public String serverLocale = "en";
}
