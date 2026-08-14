package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class CratesSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Default crate id for /sc open when no id is given."),
    })
    public String defaultCrateId = "default";

    @Comment({
            @CommentValue("Subfolder with one YAML per crate type."),
    })
    public String cratesDirectory = "crates";

    @NewLine
    @Comment({@CommentValue("Storage backend for locations, keys, player stats.")})
    public DatabaseSettings database = new DatabaseSettings();

    @NewLine
    public MessagesSettings messages = new MessagesSettings();

    @NewLine
    @Comment({
            @CommentValue("Extra commands that run the same as /soulcrates."),
    })
    public java.util.List<String> commandAliases = java.util.List.of("sc", "crates");

    @NewLine
    @Comment({
            @CommentValue("Opening session lock timeout in seconds (anti-dupe)."),
    })
    public int sessionTimeoutSeconds = 120;

    @NewLine
    public IdleDisplaySettings idleDisplay = new IdleDisplaySettings();

    @NewLine
    public BroadcastSettings broadcast = new BroadcastSettings();

    @NewLine
    public PremiumOpeningSettings premiumOpening = new PremiumOpeningSettings();

    @NewLine
    public ClaimSettings claim = new ClaimSettings();

    @NewLine
    public LastWinnerSettings lastWinner = new LastWinnerSettings();

    @NewLine
    public CrateShopSettings shop = new CrateShopSettings();

    @NewLine
    @Comment({
            @CommentValue("Redis pub/sub mirror for virtual keys and pity (MYSQL networks)."),
    })
    public RedisSettings redis = new RedisSettings();

    @NewLine
    public PhysicalCrateSettings physicalCrates = new PhysicalCrateSettings();

    @Comment({
            @CommentValue("Permission node prefix for crate-specific nodes."),
    })
    public String permissionPrefix = "soulcrates.crate.";
}
