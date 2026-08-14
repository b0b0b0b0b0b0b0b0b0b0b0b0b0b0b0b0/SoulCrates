package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class IdleDisplaySettings extends YamlSerializable {

    @Comment({
            @CommentValue("Spawn idle models/displays at bound crate blocks."),
    })
    public boolean enabled = true;

    @Comment({
            @CommentValue("Ambient particles above bound crates."),
    })
    public boolean particles = true;

    public String particleType = "PORTAL";

    public int particleIntervalTicks = 40;

    public int particleCount = 3;

    @Comment({
            @CommentValue("Tick interval for per-crate idle effect patterns."),
    })
    public int idleEffectTickInterval = 2;

    @Comment({
            @CommentValue("When a bound crate defines idleEffects, skip global ambient particles for it."),
    })
    public boolean preferPerCrateIdleEffects = true;

    @Comment({
            @CommentValue("Play sound when a player interacts with a bound crate."),
    })
    public boolean interactSound = true;

    public String interactSoundName = "BLOCK_ENDER_CHEST_OPEN";

    public float interactSoundVolume = 0.8f;

    public float interactSoundPitch = 1.1f;

    @net.elytrium.serializer.annotations.NewLine
    public HologramSettings hologram = new HologramSettings();
}
