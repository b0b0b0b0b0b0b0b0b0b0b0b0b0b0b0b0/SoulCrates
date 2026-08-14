package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class ClaimSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Pending rewards queue when inventory is full or by policy."),
    })
    public boolean enabled = true;

    @Comment({
            @CommentValue("Always store rewards in claim menu instead of direct delivery."),
    })
    public boolean alwaysToClaim = false;

    @Comment({
            @CommentValue("Queue item rewards when inventory has no space."),
    })
    public boolean overflowToClaim = true;

    @Comment({
            @CommentValue("Allow /sc giveclaim for offline players (stored in DB)."),
    })
    public boolean offlineSupport = true;

    public int maxPendingPerPlayer = 500;
}
