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

    public List<String> grants = new ArrayList<>(List.of("IRON_INGOT:1"));

    public List<String> commands = new ArrayList<>();
}
