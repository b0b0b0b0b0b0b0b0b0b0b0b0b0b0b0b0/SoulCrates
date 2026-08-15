package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class GuiSpinnerSettings extends YamlSerializable {

    public int size = 27;

    @Comment({
            @CommentValue("Winning track slot under the pointer when the roulette stops. Default slot 13."),
    })
    public int winSlot = 13;

    public int pointerSlot = 4;

    public String pointerMaterial = "PRISMARINE_SHARD";

    public String fillerMaterial = "BLACK_STAINED_GLASS_PANE";

    @Comment({
            @CommentValue("Track slots 10-16 scroll as one window. Odd tape cells are empty gaps between items."),
    })
    public int minSpinSteps = 48;

    public int maxSpinSteps = 68;

    public int minSpinIntervalTicks = 1;

    public int maxSpinIntervalTicks = 11;

    public int finishPauseTicks = 22;

    @Comment({
            @CommentValue("Show rare rewards on the tape more often than real odds (bait / near-miss)."),
    })
    public boolean baitEnabled = true;

    public double baitRareBoost = 6.0;

    public double tapeRareBoost = 3.5;

    @Comment({
            @CommentValue("Tick sound when a new reward passes under the pointer."),
    })
    public boolean spinTickSound = true;

    public String spinTickSoundName = "BLOCK_NOTE_BLOCK_HAT";

    public float spinTickSoundVolume = 0.55f;

    public float spinTickSoundPitch = 1.35f;

    @Comment({
            @CommentValue("Sound when the roulette stops on the winning item."),
    })
    public boolean winLockSound = true;

    public String winLockSoundName = "BLOCK_NOTE_BLOCK_PLING";

    public float winLockSoundVolume = 0.75f;

    public float winLockSoundPitch = 1.0f;
}
