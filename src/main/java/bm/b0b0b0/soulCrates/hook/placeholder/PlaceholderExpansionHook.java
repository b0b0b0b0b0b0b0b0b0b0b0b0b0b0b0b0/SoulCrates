package bm.b0b0b0.soulCrates.hook.placeholder;

import bm.b0b0b0.soulCrates.hook.PluginHook;
import bm.b0b0b0.soulCrates.placeholder.SoulCratesPlaceholderExpansion;
import bm.b0b0b0.soulCrates.service.CrateService;
import java.util.function.Supplier;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlaceholderExpansionHook implements PluginHook {

    private final JavaPlugin plugin;
    private final Supplier<CrateService> crateServiceSupplier;
    private SoulCratesPlaceholderExpansion expansion;
    private boolean enabled;

    public PlaceholderExpansionHook(JavaPlugin plugin, Supplier<CrateService> crateServiceSupplier) {
        this.plugin = plugin;
        this.crateServiceSupplier = crateServiceSupplier;
    }

    @Override
    public String id() {
        return "placeholderapi";
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public void load(JavaPlugin plugin) {
        CrateService crateService = crateServiceSupplier.get();
        if (crateService == null) {
            return;
        }
        expansion = new SoulCratesPlaceholderExpansion(plugin, crateService);
        expansion.register();
        enabled = true;
    }

    @Override
    public void unload() {
        if (expansion != null) {
            expansion.unregister();
        }
        enabled = false;
        expansion = null;
    }
}
