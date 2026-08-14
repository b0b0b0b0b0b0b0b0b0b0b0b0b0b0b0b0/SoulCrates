package bm.b0b0b0.soulCrates.config.settings;

import java.util.ArrayList;
import java.util.List;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class AlternativeRewardSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Deliver this fallback when the primary reward cannot be won (limits, permissions)."),
    })
    public boolean enabled = false;

    public String displayName = "Consolation Prize";

    public String material = "IRON_INGOT";

    @Comment({
            @CommentValue("Physical items only: MATERIAL:amount. See reward.commands for money/ranks."),
    })
    public List<String> grants = new ArrayList<>(List.of("IRON_INGOT:1"));

    @Comment({
            @CommentValue("Console commands. Placeholders: {player}, {uuid}, {crate}, {reward}. Example: eco give {player} 500"),
    })
    public List<String> commands = new ArrayList<>();
}
