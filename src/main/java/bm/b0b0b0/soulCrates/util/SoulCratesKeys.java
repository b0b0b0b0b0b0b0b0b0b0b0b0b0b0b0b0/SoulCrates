package bm.b0b0b0.soulCrates.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class SoulCratesKeys {

    public static final String KEY_ID = "key";
    public static final String CRATE_ID = "crate";
    public static final String LOOTBOX_ID = "lootbox";
    public static final String LOOTBOX_RARITY = "lootbox_rarity";
    public static final String KEY_RARITY = "key_rarity";

    private SoulCratesKeys() {
    }

    public static NamespacedKey keyType(Plugin plugin) {
        return new NamespacedKey(plugin, KEY_ID);
    }

    public static NamespacedKey crateBlock(Plugin plugin) {
        return new NamespacedKey(plugin, CRATE_ID);
    }

    public static NamespacedKey lootBoxType(Plugin plugin) {
        return new NamespacedKey(plugin, LOOTBOX_ID);
    }

    public static NamespacedKey lootBoxRarity(Plugin plugin) {
        return new NamespacedKey(plugin, LOOTBOX_RARITY);
    }

    public static NamespacedKey keyRarity(Plugin plugin) {
        return new NamespacedKey(plugin, KEY_RARITY);
    }
}
