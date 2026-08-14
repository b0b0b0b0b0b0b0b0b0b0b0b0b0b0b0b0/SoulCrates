package bm.b0b0b0.soulCrates;

import bm.b0b0b0.soulCrates.bootstrap.SoulCratesCore;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoulCrates extends JavaPlugin {

    private SoulCratesCore core;

    @Override
    public void onEnable() {
        core = new SoulCratesCore(this);
        core.enable();
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.disable();
            core = null;
        }
    }

    public SoulCratesCore core() {
        return core;
    }
}
