package bm.b0b0b0.soulCrates.config.settings;

import java.util.ArrayList;
import java.util.List;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class RewardEntrySettings extends YamlSerializable {

    @Comment({
            @CommentValue("Unique reward id inside this crate."),
    })
    public String id = "common";

    @Comment({
            @CommentValue("Rarity tier id from crate rarities list. Empty = default pool."),
    })
    public String rarity = "";

    @Comment({
            @CommentValue("Roll weight. Higher = more common."),
    })
    public double weight = 10.0;

    @Comment({
            @CommentValue("Display name in preview/spinner GUI."),
    })
    public String displayName = "Common Reward";

    @Comment({
            @CommentValue("Preview icon material."),
    })
    public String material = "DIAMOND";

    public int customModelData = -1;

    @Comment({
            @CommentValue("Items given: MATERIAL:amount or vault:100"),
    })
    public List<String> grants = new ArrayList<>(List.of("DIAMOND:1"));

    @Comment({
            @CommentValue("Console commands. Placeholders: {player}, {crate}, {reward}"),
    })
    public List<String> commands = new ArrayList<>();

    @Comment({
            @CommentValue("Eligible for pity system when pity.reward-id matches this id."),
    })
    public boolean pityEligible = false;

    @Comment({
            @CommentValue("Broadcast this reward to the whole server on win."),
    })
    public boolean broadcast = false;

    @Comment({
            @CommentValue("Max wins per player for this reward. -1 = unlimited."),
    })
    public int playerWinLimit = -1;

    @Comment({
            @CommentValue("Max wins server-wide for this reward. -1 = unlimited."),
    })
    public int globalWinLimit = -1;

    public int winLimitCooldownSeconds = 0;

    public int globalWinLimitCooldownSeconds = 0;

    @Comment({
            @CommentValue("Unix epoch ms when reward expires. 0 = never."),
    })
    public long expiresAtEpochMs = 0L;

    @Comment({
            @CommentValue("Keys required to redeem in SELECT mode. 0 = use crate opening.keysRequired."),
    })
    public int requiredKeys = 0;

    public java.util.List<String> requiredPermissions = new java.util.ArrayList<>();

    public java.util.List<String> restrictedPermissions = new java.util.ArrayList<>();

    public boolean enabled = true;

    @net.elytrium.serializer.annotations.NewLine
    public AlternativeRewardSettings alternative = new AlternativeRewardSettings();
}
