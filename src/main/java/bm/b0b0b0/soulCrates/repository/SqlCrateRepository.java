package bm.b0b0b0.soulCrates.repository;

import bm.b0b0b0.soulCrates.config.settings.DatabaseSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import bm.b0b0b0.soulCrates.model.PendingClaim;
import bm.b0b0b0.soulCrates.model.WinnerEntry;
import bm.b0b0b0.soulCrates.util.RewardSnapshotCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import org.bukkit.Location;

public final class SqlCrateRepository implements CrateRepository, AutoCloseable {

    private final HikariDataSource dataSource;
    private final ExecutorService executor;
    private final Logger logger;
    private final SqlDialect dialect;

    public SqlCrateRepository(Path dataFolder, DatabaseSettings settings, Logger logger) {
        this.logger = logger;
        this.dialect = new SqlDialect(settings.mode);
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "SoulCrates-SQL");
            thread.setDaemon(true);
            return thread;
        });
        HikariConfig config = new HikariConfig();
        if ("MYSQL".equalsIgnoreCase(settings.mode)) {
            config.setJdbcUrl("jdbc:mysql://" + settings.mysqlHost + ":" + settings.mysqlPort + "/" + settings.mysqlDatabase
                    + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8");
            config.setUsername(settings.mysqlUsername);
            config.setPassword(settings.mysqlPassword);
        } else {
            Path sqlitePath = dataFolder.resolve(settings.sqliteFile);
            sqlitePath.getParent().toFile().mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + sqlitePath.toAbsolutePath());
        }
        config.setMaximumPoolSize(Math.max(1, settings.poolSize));
        config.setConnectionTimeout(Math.max(1000, settings.connectionTimeoutMillis));
        config.setMaxLifetime(Math.max(30000, settings.maxLifetimeMillis));
        config.setPoolName("SoulCrates-Hikari");
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public CompletableFuture<Void> migrate() {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                if (dialect.mysql()) {
                    migrateMysql(statement);
                } else {
                    migrateSqlite(statement);
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Crate database migration failed", exception);
            }
        }, executor);
    }

    private void migrateSqlite(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_virtual_keys (
                    player_uuid TEXT NOT NULL,
                    crate_id TEXT NOT NULL,
                    amount INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (player_uuid, crate_id)
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_pity (
                    player_uuid TEXT NOT NULL,
                    crate_id TEXT NOT NULL,
                    counter INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (player_uuid, crate_id)
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_opens (
                    player_uuid TEXT NOT NULL,
                    crate_id TEXT NOT NULL,
                    total INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (player_uuid, crate_id)
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_locations (
                    world TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    crate_id TEXT NOT NULL,
                    PRIMARY KEY (world, x, y, z)
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_last_reward (
                    player_uuid TEXT NOT NULL,
                    crate_id TEXT NOT NULL,
                    reward_id TEXT NOT NULL,
                    opened_at INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, crate_id)
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_npc_bindings (
                    npc_id INTEGER NOT NULL PRIMARY KEY,
                    crate_id TEXT NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_pending_claims (
                    claim_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    crate_id TEXT NOT NULL,
                    reward_json TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_winner_history (
                    entry_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    crate_id TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    reward_id TEXT NOT NULL,
                    reward_display TEXT NOT NULL,
                    won_at INTEGER NOT NULL
                )
                """);
    }

    private void migrateMysql(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_virtual_keys (
                    player_uuid VARCHAR(36) NOT NULL,
                    crate_id VARCHAR(64) NOT NULL,
                    amount INT NOT NULL DEFAULT 0,
                    PRIMARY KEY (player_uuid, crate_id)
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_pity (
                    player_uuid VARCHAR(36) NOT NULL,
                    crate_id VARCHAR(64) NOT NULL,
                    counter INT NOT NULL DEFAULT 0,
                    PRIMARY KEY (player_uuid, crate_id)
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_opens (
                    player_uuid VARCHAR(36) NOT NULL,
                    crate_id VARCHAR(64) NOT NULL,
                    total INT NOT NULL DEFAULT 0,
                    PRIMARY KEY (player_uuid, crate_id)
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_locations (
                    world VARCHAR(64) NOT NULL,
                    x INT NOT NULL,
                    y INT NOT NULL,
                    z INT NOT NULL,
                    crate_id VARCHAR(64) NOT NULL,
                    PRIMARY KEY (world, x, y, z)
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_last_reward (
                    player_uuid VARCHAR(36) NOT NULL,
                    crate_id VARCHAR(64) NOT NULL,
                    reward_id VARCHAR(128) NOT NULL,
                    opened_at BIGINT NOT NULL,
                    PRIMARY KEY (player_uuid, crate_id)
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_npc_bindings (
                    npc_id INT NOT NULL PRIMARY KEY,
                    crate_id VARCHAR(64) NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_pending_claims (
                    claim_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    crate_id VARCHAR(64) NOT NULL,
                    reward_json TEXT NOT NULL,
                    created_at BIGINT NOT NULL,
                    INDEX idx_pending_player (player_uuid)
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS soulcrates_winner_history (
                    entry_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    crate_id VARCHAR(64) NOT NULL,
                    player_uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(64) NOT NULL,
                    reward_id VARCHAR(128) NOT NULL,
                    reward_display VARCHAR(256) NOT NULL,
                    won_at BIGINT NOT NULL,
                    INDEX idx_winner_crate (crate_id, won_at)
                )
                """);
    }

    @Override
    public CompletableFuture<Integer> loadVirtualKeys(UUID playerId, String crateId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                         "SELECT amount FROM soulcrates_virtual_keys WHERE player_uuid = ? AND crate_id = ?")) {
                statement.setString(1, playerId.toString());
                statement.setString(2, crateId.toLowerCase());
                try (var resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("amount");
                    }
                }
            } catch (SQLException exception) {
                logger.warning("Failed to load virtual keys: " + exception.getMessage());
            }
            return 0;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveVirtualKeys(UUID playerId, String crateId, int amount) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(dialect.upsertVirtualKeys())) {
                statement.setString(1, playerId.toString());
                statement.setString(2, crateId.toLowerCase());
                statement.setInt(3, Math.max(0, amount));
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to save virtual keys", exception);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> loadPityCounter(UUID playerId, String crateId) {
        return loadCounter("soulcrates_pity", "counter", playerId, crateId);
    }

    @Override
    public CompletableFuture<Void> savePityCounter(UUID playerId, String crateId, int counter) {
        return saveCounter("soulcrates_pity", "counter", playerId, crateId, counter);
    }

    @Override
    public CompletableFuture<Void> incrementPityCounter(UUID playerId, String crateId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(dialect.upsertIncrementPity())) {
                statement.setString(1, playerId.toString());
                statement.setString(2, crateId.toLowerCase());
                statement.executeUpdate();
            } catch (SQLException exception) {
                logger.warning("Failed to increment pity counter: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> loadOpens(UUID playerId, String crateId) {
        return loadCounter("soulcrates_opens", "total", playerId, crateId);
    }

    @Override
    public CompletableFuture<Void> incrementOpens(UUID playerId, String crateId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(dialect.upsertIncrementOpens())) {
                statement.setString(1, playerId.toString());
                statement.setString(2, crateId.toLowerCase());
                statement.executeUpdate();
            } catch (SQLException exception) {
                logger.warning("Failed to increment opens: " + exception.getMessage());
            }
        }, executor);
    }

    private CompletableFuture<Integer> loadCounter(String table, String column, UUID playerId, String crateId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                         "SELECT " + column + " FROM " + table + " WHERE player_uuid = ? AND crate_id = ?")) {
                statement.setString(1, playerId.toString());
                statement.setString(2, crateId.toLowerCase());
                try (var resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(column);
                    }
                }
            } catch (SQLException exception) {
                logger.warning("Failed to load counter from " + table + ": " + exception.getMessage());
            }
            return 0;
        }, executor);
    }

    private CompletableFuture<Void> saveCounter(String table, String column, UUID playerId, String crateId, int value) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(dialect.upsertCounter(table, column))) {
                statement.setString(1, playerId.toString());
                statement.setString(2, crateId.toLowerCase());
                statement.setInt(3, Math.max(0, value));
                statement.executeUpdate();
            } catch (SQLException exception) {
                logger.warning("Failed to save counter to " + table + ": " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Map<String, String>> loadAllLocations() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> locations = new java.util.HashMap<>();
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                         "SELECT world, x, y, z, crate_id FROM soulcrates_locations");
                 var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String key = resultSet.getString("world").toLowerCase()
                            + ":" + resultSet.getInt("x")
                            + ":" + resultSet.getInt("y")
                            + ":" + resultSet.getInt("z");
                    locations.put(key, resultSet.getString("crate_id").toLowerCase());
                }
            } catch (SQLException exception) {
                logger.warning("Failed to load crate locations: " + exception.getMessage());
            }
            return locations;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveLocation(org.bukkit.Location location, String crateId) {
        return CompletableFuture.runAsync(() -> {
            if (location.getWorld() == null) {
                return;
            }
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(dialect.upsertLocation())) {
                statement.setString(1, location.getWorld().getName());
                statement.setInt(2, location.getBlockX());
                statement.setInt(3, location.getBlockY());
                statement.setInt(4, location.getBlockZ());
                statement.setString(5, crateId.toLowerCase());
                statement.executeUpdate();
            } catch (SQLException exception) {
                logger.warning("Failed to save crate location: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteLocation(org.bukkit.Location location) {
        return CompletableFuture.runAsync(() -> {
            if (location.getWorld() == null) {
                return;
            }
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                         "DELETE FROM soulcrates_locations WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                statement.setString(1, location.getWorld().getName());
                statement.setInt(2, location.getBlockX());
                statement.setInt(3, location.getBlockY());
                statement.setInt(4, location.getBlockZ());
                statement.executeUpdate();
            } catch (SQLException exception) {
                logger.warning("Failed to delete crate location: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Map<String, Integer>> loadAllOpens(UUID playerId) {
        return loadAllCounters("soulcrates_opens", "total", playerId);
    }

    @Override
    public CompletableFuture<Map<String, Integer>> loadAllPityCounters(UUID playerId) {
        return loadAllCounters("soulcrates_pity", "counter", playerId);
    }

    @Override
    public CompletableFuture<Map<String, String>> loadLastRewards(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> values = new java.util.HashMap<>();
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                         "SELECT crate_id, reward_id FROM soulcrates_last_reward WHERE player_uuid = ?")) {
                statement.setString(1, playerId.toString());
                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        values.put(resultSet.getString("crate_id").toLowerCase(), resultSet.getString("reward_id"));
                    }
                }
            } catch (SQLException exception) {
                logger.warning("Failed to load last rewards: " + exception.getMessage());
            }
            return values;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> recordLastReward(UUID playerId, String crateId, String rewardId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(dialect.upsertLastReward())) {
                statement.setString(1, playerId.toString());
                statement.setString(2, crateId.toLowerCase());
                statement.setString(3, rewardId.toLowerCase());
                statement.setLong(4, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (SQLException exception) {
                logger.warning("Failed to record last reward: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Map<Integer, String>> loadAllNpcBindings() {
        return CompletableFuture.supplyAsync(() -> {
            Map<Integer, String> values = new java.util.HashMap<>();
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                         "SELECT npc_id, crate_id FROM soulcrates_npc_bindings")) {
                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        values.put(resultSet.getInt("npc_id"), resultSet.getString("crate_id").toLowerCase());
                    }
                }
            } catch (SQLException exception) {
                logger.warning("Failed to load NPC bindings: " + exception.getMessage());
            }
            return values;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveNpcBinding(int npcId, String crateId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(dialect.upsertNpcBinding())) {
                statement.setInt(1, npcId);
                statement.setString(2, crateId.toLowerCase());
                statement.executeUpdate();
            } catch (SQLException exception) {
                logger.warning("Failed to save NPC binding: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteNpcBinding(int npcId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                         "DELETE FROM soulcrates_npc_bindings WHERE npc_id = ?")) {
                statement.setInt(1, npcId);
                statement.executeUpdate();
            } catch (SQLException exception) {
                logger.warning("Failed to delete NPC binding: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Long> enqueueClaim(UUID playerId, String crateId, String rewardJson) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                         """
                                 INSERT INTO soulcrates_pending_claims (player_uuid, crate_id, reward_json, created_at)
                                 VALUES (?, ?, ?, ?)
                                 """,
                         Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, crateId.toLowerCase());
                statement.setString(3, rewardJson);
                statement.setLong(4, System.currentTimeMillis());
                statement.executeUpdate();
                try (var keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getLong(1);
                    }
                }
            } catch (SQLException exception) {
                logger.warning("Failed to enqueue claim: " + exception.getMessage());
            }
            return -1L;
        }, executor);
    }

    @Override
    public CompletableFuture<List<PendingClaim>> loadPendingClaims(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            List<PendingClaim> claims = new ArrayList<>();
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                         """
                                 SELECT claim_id, crate_id, reward_json, created_at
                                 FROM soulcrates_pending_claims
                                 WHERE player_uuid = ?
                                 ORDER BY claim_id ASC
                                 """)) {
                statement.setString(1, playerId.toString());
                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        claims.add(new PendingClaim(
                                resultSet.getLong("claim_id"),
                                playerId,
                                resultSet.getString("crate_id"),
                                RewardSnapshotCodec.decode(resultSet.getString("reward_json")),
                                resultSet.getLong("created_at")
                        ));
                    }
                }
            } catch (SQLException exception) {
                logger.warning("Failed to load pending claims: " + exception.getMessage());
            }
            return claims;
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> countPendingClaims(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                         "SELECT COUNT(*) FROM soulcrates_pending_claims WHERE player_uuid = ?")) {
                statement.setString(1, playerId.toString());
                try (var resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1);
                    }
                }
            } catch (SQLException exception) {
                logger.warning("Failed to count pending claims: " + exception.getMessage());
            }
            return 0;
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> deleteClaim(long claimId, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                         "DELETE FROM soulcrates_pending_claims WHERE claim_id = ? AND player_uuid = ?")) {
                statement.setLong(1, claimId);
                statement.setString(2, playerId.toString());
                return statement.executeUpdate() > 0;
            } catch (SQLException exception) {
                logger.warning("Failed to delete claim: " + exception.getMessage());
                return false;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> recordWinner(String crateId, UUID playerId, String playerName, String rewardId, String rewardDisplay, long wonAt, int maxHistory) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (var insert = connection.prepareStatement(
                        """
                                INSERT INTO soulcrates_winner_history (crate_id, player_uuid, player_name, reward_id, reward_display, won_at)
                                VALUES (?, ?, ?, ?, ?, ?)
                                """)) {
                    insert.setString(1, crateId.toLowerCase());
                    insert.setString(2, playerId.toString());
                    insert.setString(3, playerName);
                    insert.setString(4, rewardId.toLowerCase());
                    insert.setString(5, rewardDisplay);
                    insert.setLong(6, wonAt);
                    insert.executeUpdate();
                }
                int safeLimit = Math.max(1, Math.min(20, maxHistory));
                try (var prune = connection.prepareStatement(dialect.pruneWinnerHistory())) {
                    prune.setString(1, crateId.toLowerCase());
                    prune.setString(2, crateId.toLowerCase());
                    prune.setInt(3, safeLimit);
                    prune.executeUpdate();
                }
            } catch (SQLException exception) {
                logger.warning("Failed to record winner: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<WinnerEntry>> loadWinnerHistory(String crateId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<WinnerEntry> entries = new ArrayList<>();
            int safeLimit = Math.max(1, Math.min(20, limit));
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                         """
                                 SELECT player_uuid, player_name, reward_id, reward_display, won_at
                                 FROM soulcrates_winner_history
                                 WHERE crate_id = ?
                                 ORDER BY won_at DESC, entry_id DESC
                                 LIMIT ?
                                 """)) {
                statement.setString(1, crateId.toLowerCase());
                statement.setInt(2, safeLimit);
                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        entries.add(new WinnerEntry(
                                crateId.toLowerCase(),
                                UUID.fromString(resultSet.getString("player_uuid")),
                                resultSet.getString("player_name"),
                                resultSet.getString("reward_id"),
                                resultSet.getString("reward_display"),
                                resultSet.getLong("won_at")
                        ));
                    }
                }
            } catch (SQLException exception) {
                logger.warning("Failed to load winner history: " + exception.getMessage());
            }
            return entries;
        }, executor);
    }

    private CompletableFuture<Map<String, Integer>> loadAllCounters(String table, String column, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> values = new java.util.HashMap<>();
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                         "SELECT crate_id, " + column + " FROM " + table + " WHERE player_uuid = ?")) {
                statement.setString(1, playerId.toString());
                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        values.put(resultSet.getString("crate_id").toLowerCase(), resultSet.getInt(column));
                    }
                }
            } catch (SQLException exception) {
                logger.warning("Failed to load counters from " + table + ": " + exception.getMessage());
            }
            return values;
        }, executor);
    }

    @Override
    public void close() {
        executor.shutdownNow();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
