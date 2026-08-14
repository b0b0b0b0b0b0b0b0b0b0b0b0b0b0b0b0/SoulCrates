package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class DatabaseSettings extends YamlSerializable {

    @Comment({
            @CommentValue("SQLITE or MYSQL"),
    })
    public String mode = "SQLITE";

    @Comment({
            @CommentValue("Relative to plugin data folder for SQLITE."),
    })
    public String sqliteFile = "data/crates.db";

    public String mysqlHost = "127.0.0.1";
    public int mysqlPort = 3306;
    public String mysqlDatabase = "soulcrates";
    public String mysqlUsername = "root";
    public String mysqlPassword = "";

    @Comment({
            @CommentValue("HikariCP pool size."),
    })
    public int poolSize = 4;

    public int connectionTimeoutMillis = 10000;
    public int maxLifetimeMillis = 1800000;
}
