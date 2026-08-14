package bm.b0b0b0.soulCrates.repository;

public final class SqlDialect {

    private final boolean mysql;

    public SqlDialect(String mode) {
        this.mysql = "MYSQL".equalsIgnoreCase(mode);
    }

    public boolean mysql() {
        return mysql;
    }

    public String upsertVirtualKeys() {
        if (mysql) {
            return """
                    INSERT INTO soulcrates_virtual_keys (player_uuid, crate_id, amount)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE amount = VALUES(amount)
                    """;
        }
        return """
                INSERT INTO soulcrates_virtual_keys (player_uuid, crate_id, amount)
                VALUES (?, ?, ?)
                ON CONFLICT(player_uuid, crate_id) DO UPDATE SET amount = excluded.amount
                """;
    }

    public String upsertIncrementPity() {
        if (mysql) {
            return """
                    INSERT INTO soulcrates_pity (player_uuid, crate_id, counter)
                    VALUES (?, ?, 1)
                    ON DUPLICATE KEY UPDATE counter = counter + 1
                    """;
        }
        return """
                INSERT INTO soulcrates_pity (player_uuid, crate_id, counter)
                VALUES (?, ?, 1)
                ON CONFLICT(player_uuid, crate_id) DO UPDATE SET counter = counter + 1
                """;
    }

    public String upsertIncrementOpens() {
        if (mysql) {
            return """
                    INSERT INTO soulcrates_opens (player_uuid, crate_id, total)
                    VALUES (?, ?, 1)
                    ON DUPLICATE KEY UPDATE total = total + 1
                    """;
        }
        return """
                INSERT INTO soulcrates_opens (player_uuid, crate_id, total)
                VALUES (?, ?, 1)
                ON CONFLICT(player_uuid, crate_id) DO UPDATE SET total = total + 1
                """;
    }

    public String upsertCounter(String table, String column) {
        if (mysql) {
            return """
                    INSERT INTO %s (player_uuid, crate_id, %s)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE %s = VALUES(%s)
                    """.formatted(table, column, column, column);
        }
        return """
                INSERT INTO %s (player_uuid, crate_id, %s)
                VALUES (?, ?, ?)
                ON CONFLICT(player_uuid, crate_id) DO UPDATE SET %s = excluded.%s
                """.formatted(table, column, column, column);
    }

    public String upsertLocation() {
        if (mysql) {
            return """
                    INSERT INTO soulcrates_locations (world, x, y, z, crate_id)
                    VALUES (?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE crate_id = VALUES(crate_id)
                    """;
        }
        return """
                INSERT INTO soulcrates_locations (world, x, y, z, crate_id)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(world, x, y, z) DO UPDATE SET crate_id = excluded.crate_id
                """;
    }

    public String upsertLastReward() {
        if (mysql) {
            return """
                    INSERT INTO soulcrates_last_reward (player_uuid, crate_id, reward_id, opened_at)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE reward_id = VALUES(reward_id), opened_at = VALUES(opened_at)
                    """;
        }
        return """
                INSERT INTO soulcrates_last_reward (player_uuid, crate_id, reward_id, opened_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(player_uuid, crate_id) DO UPDATE SET
                    reward_id = excluded.reward_id,
                    opened_at = excluded.opened_at
                """;
    }

    public String upsertNpcBinding() {
        if (mysql) {
            return """
                    INSERT INTO soulcrates_npc_bindings (npc_id, crate_id)
                    VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE crate_id = VALUES(crate_id)
                    """;
        }
        return """
                INSERT INTO soulcrates_npc_bindings (npc_id, crate_id)
                VALUES (?, ?)
                ON CONFLICT(npc_id) DO UPDATE SET crate_id = excluded.crate_id
                """;
    }

    public String pruneWinnerHistory() {
        if (mysql) {
            return """
                    DELETE FROM soulcrates_winner_history
                    WHERE crate_id = ? AND entry_id NOT IN (
                        SELECT entry_id FROM (
                            SELECT entry_id FROM soulcrates_winner_history
                            WHERE crate_id = ?
                            ORDER BY won_at DESC, entry_id DESC
                            LIMIT ?
                        ) recent
                    )
                    """;
        }
        return """
                DELETE FROM soulcrates_winner_history
                WHERE crate_id = ? AND entry_id NOT IN (
                    SELECT entry_id FROM soulcrates_winner_history
                    WHERE crate_id = ?
                    ORDER BY won_at DESC, entry_id DESC
                    LIMIT ?
                )
                """;
    }
}
