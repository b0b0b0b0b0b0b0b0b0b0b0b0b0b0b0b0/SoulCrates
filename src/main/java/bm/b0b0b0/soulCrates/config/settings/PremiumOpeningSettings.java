package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class PremiumOpeningSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Skip animation phases but keep reroll GUI when enabled on crate."),
    })
    public String skipAnimationPermission = "soulcrates.open.skip";

    @Comment({
            @CommentValue("Instant open: no animation and no display spawn."),
    })
    public String instantOpenPermission = "soulcrates.open.instant";

    @Comment({
            @CommentValue("Allow /sc open <crate> <amount> and preview multi-open buttons."),
    })
    public String multiOpenPermission = "soulcrates.open.multi";

    public int maxMultiOpen = 10;

    public boolean instantSkipsReroll = true;

    public boolean multiOpenSkipsReroll = true;

    public boolean multiOpenSkipsAnimation = true;
}
