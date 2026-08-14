package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class OpenCostSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Allow opening/redeeming without keys by paying Vault."),
    })
    public boolean enabled = false;

    @Comment({
            @CommentValue("Vault price when keys are missing. 0 = free fallback."),
    })
    public double vaultPrice = 0.0;

    @Comment({
            @CommentValue("If true, keys are consumed first; cost applies only when keys are insufficient."),
    })
    public boolean keysFirst = true;
}
