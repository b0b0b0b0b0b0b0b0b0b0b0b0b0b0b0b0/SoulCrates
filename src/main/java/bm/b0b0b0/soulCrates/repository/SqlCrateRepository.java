package bm.b0b0b0.soulCrates.repository;

import bm.b0b0b0.soulCrates.config.settings.DatabaseSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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

    public SqlCrateRepository(Path dataFolder, DatabaseSettings settings, Logger logger) {
        this.logger = logger;
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
            } catch (SQLException exception) {
                throw new IllegalStateException("Crate database migration failed", exception);
            }
        }, executor);
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
                 var statement = connection.prepareStatement("""
                         INSERT INTO soulcrates_virtual_keys (player_uuid, crate_id, amount)
                         VALUES (?, ?, ?)
                         ON CONFLICT(player_uuid, crate_id) DO UPDATE SET amount = excluded.amount
                         """)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, crateId.toLowerCase());
                statement.setInt(3, Math.max(0, amount));
                statement.executeUpdate();
            } catch (SQLException exception) {
                logger.warning("Failed to save virtual keys: " + exception.getMessage());
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
                 var statement = connection.prepareStatement("""
                         INSERT INTO soulcrates_pity (player_uuid, crate_id, counter)
                         VALUES (?, ?, 1)
                         ON CONFLICT(player_uuid, crate_id) DO UPDATE SET counter = counter + 1
                         """)) {
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
                 var statement = connection.prepareStatement("""
                         INSERT INTO soulcrates_opens (player_uuid, crate_id, total)
                         VALUES (?, ?, 1)
                         ON CONFLICT(player_uuid, crate_id) DO UPDATE SET total = total + 1
                         """)) {
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
                 var statement = connection.prepareStatement("""
                         INSERT INTO %s (player_uuid, crate_id, %s)
                         VALUES (?, ?, ?)
                         ON CONFLICT(player_uuid, crate_id) DO UPDATE SET %s = excluded.%s
                         """.formatted(table, column, column, column))) {
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
                 var statement = connection.prepareStatement("""
                         INSERT INTO soulcrates_locations (world, x, y, z, crate_id)
                         VALUES (?, ?, ?, ?, ?)
                         ON CONFLICT(world, x, y, z) DO UPDATE SET crate_id = excluded.crate_id
                         """)) {
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
                 var statement = connection.prepareStatement("""
                         INSERT INTO soulcrates_last_reward (player_uuid, crate_id, reward_id, opened_at)
                         VALUES (?, ?, ?, ?)
                         ON CONFLICT(player_uuid, crate_id) DO UPDATE SET
                             reward_id = excluded.reward_id,
                             opened_at = excluded.opened_at
                         """)) {
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
