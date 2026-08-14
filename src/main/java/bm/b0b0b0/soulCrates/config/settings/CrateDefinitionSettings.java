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
    public OpeningSettings opening = new OpeningSettings();

    @NewLine
    public KeySettings keys = new KeySettings();

    @NewLine
    public RerollSettings reroll = new RerollSettings();

    @NewLine
    public PitySettings pity = new PitySettings();

    @NewLine
    @Comment({@CommentValue("Reward pool for this crate.")})
    public List<RewardEntrySettings> rewards = defaultRewards();

    private static List<RewardEntrySettings> defaultRewards() {
        List<RewardEntrySettings> rewards = new ArrayList<>();
        RewardEntrySettings common = new RewardEntrySettings();
        common.id = "common";
        common.weight = 70.0;
        common.displayName = "Diamond Stack";
        common.material = "DIAMOND";
        common.grants = List.of("DIAMOND:3");
        RewardEntrySettings rare = new RewardEntrySettings();
        rare.id = "rare";
        rare.weight = 25.0;
        rare.displayName = "Emerald Stack";
        rare.material = "EMERALD";
        rare.grants = List.of("EMERALD:5");
        RewardEntrySettings legendary = new RewardEntrySettings();
        legendary.id = "legendary";
        legendary.weight = 5.0;
        legendary.displayName = "Netherite Ingot";
        legendary.material = "NETHERITE_INGOT";
        legendary.grants = List.of("NETHERITE_INGOT:1");
        legendary.pityEligible = true;
        legendary.broadcast = true;
        rewards.add(common);
        rewards.add(rare);
        rewards.add(legendary);
        return rewards;
    }
}
