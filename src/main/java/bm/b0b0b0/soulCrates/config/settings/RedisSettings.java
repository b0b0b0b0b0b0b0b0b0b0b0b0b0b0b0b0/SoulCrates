package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class RedisSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Enable Redis pub/sub mirror (recommended with MYSQL storage on networks)."),
    })
    public boolean enabled = false;

    public String host = "127.0.0.1";

    public int port = 6379;

    public String password = "";

    public int database = 0;

    public int timeoutMs = 2000;

    @Comment({
            @CommentValue("Pub/sub channel for virtual keys and pity cache sync."),
    })
    public String channel = "soulcrates:sync";

    public boolean pubSubEnabled = true;
}
