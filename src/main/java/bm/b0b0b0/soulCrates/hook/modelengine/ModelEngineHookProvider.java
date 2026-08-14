package bm.b0b0b0.soulCrates.hook.modelengine;

import bm.b0b0b0.soulCrates.hook.HookProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class ModelEngineHookProvider extends HookProvider<ModelEngineBridge> {

    public ModelEngineHookProvider() {
        super(
                "ModelEngine",
                "Install ModelEngine for 3D crate models.",
                "https://www.spigotmc.org/resources/model-engine.104011/",
                false
        );
    }

    @Override
    public ModelEngineBridge createHook(JavaPlugin plugin) {
        return new ModelEngineBridge(plugin);
    }
}
