package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class RerollSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Allow rerolling the reward after animation, before claim."),
    })
    public boolean enabled = false;

    @Comment({
            @CommentValue("Free rerolls per opening."),
    })
    public int freeRolls = 1;

    @Comment({
            @CommentValue("Maximum rerolls per opening including free rolls."),
    })
    public int maxRolls = 3;

    @Comment({
            @CommentValue("Vault cost per paid reroll after free rolls. 0 = free."),
    })
    public double vaultCost = 0.0;

    @Comment({
            @CommentValue("Skip reroll GUI when opening with instant permission."),
    })
    public boolean skipOnInstantOpen = true;

    @Comment({
            @CommentValue("Skip reroll GUI when opening with skip-animation permission."),
    })
    public boolean skipOnSkipAnimation = false;

    @Comment({
            @CommentValue("Skip reroll GUI during bulk / multi open."),
    })
    public boolean skipOnMultiOpen = true;

    @Comment({
            @CommentValue("Use per-rarity reroll caps from groups instead of flat maxRolls."),
    })
    public boolean useRarityGroups = false;

    @net.elytrium.serializer.annotations.NewLine
    public java.util.List<RerollGroupSettings> groups = defaultGroups();

    private static java.util.List<RerollGroupSettings> defaultGroups() {
        RerollGroupSettings common = new RerollGroupSettings();
        common.rarity = "common";
        common.rerolls = 3;
        RerollGroupSettings rare = new RerollGroupSettings();
        rare.rarity = "rare";
        rare.rerolls = 5;
        RerollGroupSettings legendary = new RerollGroupSettings();
        legendary.rarity = "legendary";
        legendary.rerolls = 10;
        return java.util.List.of(common, rare, legendary);
    }
}
