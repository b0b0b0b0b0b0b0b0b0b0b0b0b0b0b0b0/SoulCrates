package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class EngineSettings extends YamlSerializable {

    @Comment({
            @CommentValue("VANILLA_BLOCK, VANILLA_DISPLAY, or MODEL_ENGINE"),
    })
    public String type = "VANILLA_DISPLAY";

    @Comment({
            @CommentValue("Block material when engine uses blocks or display entities."),
    })
    public String blockMaterial = "ENDER_CHEST";

    @Comment({
            @CommentValue("ModelEngine blueprint id. Used when type is MODEL_ENGINE."),
    })
    public String modelId = "";

    public String idleAnimation = "idle";
    public String closeAnimation = "close";
}
