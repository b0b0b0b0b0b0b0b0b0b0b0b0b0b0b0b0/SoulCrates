package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class OpeningSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Require a physical or virtual key before opening."),
    })
    public boolean requireKey = false;

    @Comment({
            @CommentValue("Show preview GUI before opening."),
    })
    public boolean previewEnabled = true;

    @Comment({
            @CommentValue("Show confirmation GUI after preview or before open."),
    })
    public boolean confirmEnabled = true;

    @Comment({
            @CommentValue("Virtual keys consumed per open."),
    })
    public int keysRequired = 1;

    @Comment({
            @CommentValue("Cooldown between opens in seconds. 0 = disabled."),
    })
    public int cooldownSeconds = 0;

    @Comment({
            @CommentValue("Permission to open this crate. Empty = soulcrates.command.open only."),
    })
    public String permission = "";

    @Comment({
            @CommentValue("Allow multi-open bundles for this crate."),
    })
    public boolean allowMultiOpen = true;

    @NewLine
    public MassOpeningSettings massOpening = new MassOpeningSettings();
}
