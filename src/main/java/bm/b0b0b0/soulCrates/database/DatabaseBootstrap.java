package bm.b0b0b0.soulCrates.database;

import bm.b0b0b0.soulCrates.config.settings.DatabaseSettings;
import bm.b0b0b0.soulCrates.repository.CrateRepository;
import bm.b0b0b0.soulCrates.repository.SqlCrateRepository;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class DatabaseBootstrap {

    private final JavaPlugin plugin;
    private final DatabaseSettings settings;
    private SqlCrateRepository repository;

    public DatabaseBootstrap(JavaPlugin plugin, DatabaseSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public CompletableFuture<CrateRepository> start() {
        return CompletableFuture.supplyAsync(() -> {
            Path dataFolder = plugin.getDataFolder().toPath();
            repository = new SqlCrateRepository(dataFolder, settings, plugin.getLogger());
            return repository;
        }).thenCompose(repo -> repo.migrate().thenApply(ignored -> repo));
    }

    public void shutdown() {
        if (repository != null) {
            repository.close();
            repository = null;
        }
    }
}
