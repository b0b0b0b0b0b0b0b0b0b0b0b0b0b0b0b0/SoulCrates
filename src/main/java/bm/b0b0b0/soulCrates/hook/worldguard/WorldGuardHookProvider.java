package bm.b0b0b0.soulCrates.hook.worldguard;

import bm.b0b0b0.soulCrates.hook.HookProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldGuardHookProvider extends HookProvider<WorldGuardHook> {

    public WorldGuardHookProvider() {
        super(
                "WorldGuard",
                "Install WorldGuard for region-aware physical crate placement.",
                "https://enginehub.org/worldguard",
                false
        );
    }

    @Override
    public WorldGuardHook createHook(JavaPlugin plugin) {
        return new WorldGuardHook(plugin);
    }
}
