package bm.b0b0b0.soulCrates.hook.placeholder;

import bm.b0b0b0.soulCrates.hook.HookProvider;
import bm.b0b0b0.soulCrates.service.CrateService;
import java.util.function.Supplier;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlaceholderHookProvider extends HookProvider<PlaceholderExpansionHook> {

    private final Supplier<CrateService> crateServiceSupplier;

    public PlaceholderHookProvider(Supplier<CrateService> crateServiceSupplier) {
        super(
                "PlaceholderAPI",
                "Install PlaceholderAPI for crate placeholders.",
                "https://www.spigotmc.org/resources/placeholderapi.6245/",
                false
        );
        this.crateServiceSupplier = crateServiceSupplier;
    }

    @Override
    public PlaceholderExpansionHook createHook(JavaPlugin plugin) {
        return new PlaceholderExpansionHook(plugin, crateServiceSupplier);
    }
}
