package bm.b0b0b0.soulCrates.hook.modelengine;

import bm.b0b0b0.soulCrates.hook.PluginHook;
import org.bukkit.plugin.java.JavaPlugin;

public final class ModelEngineBridge implements PluginHook {

    private final JavaPlugin plugin;
    private boolean enabled;

    public ModelEngineBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getServer().getPluginManager().isPluginEnabled("ModelEngine");
    }

    @Override
    public String id() {
        return "modelengine";
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public void load(JavaPlugin plugin) {
        enabled = plugin.getServer().getPluginManager().isPluginEnabled("ModelEngine");
    }

    @Override
    public void unload() {
        enabled = false;
    }

    public JavaPlugin plugin() {
        return plugin;
    }
}
