package bm.b0b0b0.soulCrates.config.settings;

import java.util.List;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class HologramSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Floating labels above bound crate blocks."),
    })
    public boolean enabled = true;

    @Comment({
            @CommentValue("Height above block center. Lower = closer to crate."),
    })
    public double offsetY = 2.1;

    @Comment({
            @CommentValue("Horizontal offset from block center."),
    })
    public double offsetX = 0.0;

    public double offsetZ = 0.0;

    @Comment({
            @CommentValue("Gap between lines in blocks."),
    })
    public double lineSpacing = 0.25;

    @Comment({
            @CommentValue("VANILLA (TextDisplay), DECENT_HOLOGRAMS, or FANCY_HOLOGRAMS"),
    })
    public String provider = "VANILLA";

    @Comment({
            @CommentValue("CENTER, FIXED, HORIZONTAL, VERTICAL"),
    })
    public String billboard = "CENTER";

    @Comment({
            @CommentValue("true = text visible through walls. false = occluded by blocks (normal depth)."),
    })
    public boolean seeThrough = false;

    public boolean shadowed = true;

    public boolean defaultBackground = false;

    @Comment({
            @CommentValue("ARGB hex, e.g. #80000000. Used when defaultBackground is true."),
    })
    public String backgroundColor = "#00000000";

    public byte textOpacity = (byte) -1;

    public float viewRange = 64.0f;

    public float shadowRadius = 0.0f;

    public float shadowStrength = 1.0f;

    @Comment({
            @CommentValue("Placeholders: {crate}, {crate_id}, {reward:<id>}, {last_winner_reward}."),
            @CommentValue("Vanilla holograms resolve per viewer locale. DecentHolograms lines use %soulcrates_reward_<crate>_<id>% and %soulcrates_last_winner_<crate>_reward%."),
    })
    public List<String> lines = List.of(
            "<gold>{crate}</gold>",
            "<gray>Click to open</gray>"
    );
}
