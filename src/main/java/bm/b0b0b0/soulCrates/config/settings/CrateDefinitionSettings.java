package bm.b0b0b0.soulCrates.config.settings;

import java.util.ArrayList;
import java.util.List;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class CrateDefinitionSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Crate id. Should match file name without .yml"),
    })
    public String id = "default";

    public String displayName = "Default Crate";

    @NewLine
    public EngineSettings engine = new EngineSettings();

    @NewLine
    public AnimationSettings animations = new AnimationSettings();

    @NewLine
    @Comment({@CommentValue("Per-crate idle particle effects (Phoenix idle-effects). Max 12 recommended.")})
    public java.util.List<IdleEffectSettings> idleEffects = defaultIdleEffects();

    @NewLine
    @Comment({@CommentValue("World roulette position during open (BlockDisplay carousel).")})
    public AnimationDisplaySettings animationDisplay = new AnimationDisplaySettings();

    @NewLine
    public OpeningSettings opening = new OpeningSettings();

    @NewLine
    public KeySettings keys = new KeySettings();

    @NewLine
    public RerollSettings reroll = new RerollSettings();

    @NewLine
    public PitySettings pity = new PitySettings();

    @NewLine
    public LootBoxSettings lootBox = new LootBoxSettings();

    @NewLine
    @Comment({@CommentValue("Rarity tiers for this crate. Empty = flat weight pool.")})
    public java.util.List<RarityTierSettings> rarities = defaultRarities();

    @NewLine
    @Comment({@CommentValue("Reward pool for this crate.")})
    public List<RewardEntrySettings> rewards = defaultRewards();

    private static java.util.List<RarityTierSettings> defaultRarities() {
        RarityTierSettings common = new RarityTierSettings();
        common.id = "common";
        common.displayName = "Common";
        common.weight = 70.0;
        common.color = "<gray>";
        RarityTierSettings rare = new RarityTierSettings();
        rare.id = "rare";
        rare.displayName = "Rare";
        rare.weight = 25.0;
        rare.color = "<aqua>";
        RarityTierSettings legendary = new RarityTierSettings();
        legendary.id = "legendary";
        legendary.displayName = "Legendary";
        legendary.weight = 5.0;
        legendary.color = "<gold>";
        return java.util.List.of(common, rare, legendary);
    }

    private static List<RewardEntrySettings> defaultRewards() {
        List<RewardEntrySettings> rewards = new ArrayList<>();
        RewardEntrySettings common = new RewardEntrySettings();
        common.id = "common";
        common.weight = 70.0;
        common.displayName = "Diamond Stack";
        common.rarity = "common";
        common.material = "DIAMOND";
        common.grants = List.of("DIAMOND:3");
        RewardEntrySettings rare = new RewardEntrySettings();
        rare.id = "rare";
        rare.weight = 25.0;
        rare.displayName = "Emerald Stack";
        rare.rarity = "rare";
        rare.material = "EMERALD";
        rare.grants = List.of("EMERALD:5");
        RewardEntrySettings legendary = new RewardEntrySettings();
        legendary.id = "legendary";
        legendary.weight = 5.0;
        legendary.displayName = "Netherite Ingot";
        legendary.rarity = "legendary";
        legendary.material = "NETHERITE_INGOT";
        legendary.grants = List.of("NETHERITE_INGOT:1");
        legendary.pityEligible = true;
        legendary.broadcast = true;
        rewards.add(common);
        rewards.add(rare);
        rewards.add(legendary);
        return rewards;
    }

    private static java.util.List<IdleEffectSettings> defaultIdleEffects() {
        IdleEffectSettings redstone = new IdleEffectSettings();
        redstone.pattern = "DEFAULT";
        redstone.particle = "REDSTONE";
        redstone.color = "#ff0000";
        redstone.spread = 1.0;
        redstone.velocity = 0.1;
        redstone.amount = 2;
        IdleEffectSettings flame = new IdleEffectSettings();
        flame.pattern = "DEFAULT";
        flame.particle = "FLAME";
        flame.color = "#ffffff";
        flame.spread = 2.0;
        flame.velocity = 0.1;
        flame.amount = 2;
        IdleEffectSettings smoke = new IdleEffectSettings();
        smoke.pattern = "STAR";
        smoke.particle = "SMOKE";
        smoke.color = "#ffffff";
        smoke.spread = 0.0;
        smoke.velocity = 0.1;
        smoke.amount = 2;
        return java.util.List.of(redstone, flame, smoke);
    }
}
