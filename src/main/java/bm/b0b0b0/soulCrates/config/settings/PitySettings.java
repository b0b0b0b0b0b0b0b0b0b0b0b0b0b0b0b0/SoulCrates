package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class PitySettings extends YamlSerializable {

    @Comment({
            @CommentValue("Force a reward after N opens without hitting it."),
    })
    public boolean enabled = false;

    @Comment({
            @CommentValue("Opens without pity reward before guarantee."),
    })
    public int threshold = 50;

    @Comment({
            @CommentValue("Reward id from rewards list to grant on pity."),
    })
    public String rewardId = "";
}
