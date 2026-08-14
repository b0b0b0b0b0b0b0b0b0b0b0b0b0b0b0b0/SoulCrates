package bm.b0b0b0.soulCrates.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class SoulCratesKeys {

    public static final String KEY_ID = "key";
    public static final String CRATE_ID = "crate";

    private SoulCratesKeys() {
    }

    public static NamespacedKey keyType(Plugin plugin) {
        return new NamespacedKey(plugin, KEY_ID);
    }

    public static NamespacedKey crateBlock(Plugin plugin) {
        return new NamespacedKey(plugin, CRATE_ID);
    }
}
