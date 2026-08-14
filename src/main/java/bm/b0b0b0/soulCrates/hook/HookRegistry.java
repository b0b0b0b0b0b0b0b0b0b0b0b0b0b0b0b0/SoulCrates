package bm.b0b0b0.soulCrates.hook;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class HookRegistry {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final List<HookProvider<?>> providers = new ArrayList<>();
    private final Map<Class<? extends PluginHook>, PluginHook> hooks = new LinkedHashMap<>();
    private final List<Runnable> onLoadTasks = new ArrayList<>();

    public HookRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void registerProvider(HookProvider<?> provider) {
        providers.add(provider);
    }

    public void onLoad(Runnable task) {
        if (task != null) {
            onLoadTasks.add(task);
        }
    }

    public void registerHooks() {
        for (HookProvider<?> provider : providers) {
            if (!provider.isAvailable(plugin)) {
                if (!provider.silent()) {
                    logger.info(provider.pluginName() + " is not installed. " + provider.description());
                    if (!provider.url().isEmpty()) {
                        logger.info("-> " + provider.url());
                    }
                }
                continue;
            }
            if (!provider.isEnabled(plugin)) {
                logger.warning(provider.pluginName() + " found but disabled.");
                continue;
            }
            logger.info(provider.pluginName() + " found. Preparing hook...");
            try {
                PluginHook hook = provider.createHook(plugin);
                hook.load(plugin);
                hooks.put(hook.getClass(), hook);
            } catch (Exception exception) {
                logger.warning("Failed to initialize hook " + provider.pluginName() + ": " + exception.getMessage());
            }
        }
    }

    public void loadHooks() {
        for (Runnable task : onLoadTasks) {
            try {
                task.run();
            } catch (Exception exception) {
                logger.warning("Hook onLoad task failed: " + exception.getMessage());
            }
        }
        onLoadTasks.clear();
    }

    public void unloadHooks() {
        for (PluginHook hook : hooks.values()) {
            try {
                hook.unload();
            } catch (Exception exception) {
                logger.warning("Failed to unload hook " + hook.id() + ": " + exception.getMessage());
            }
        }
        hooks.clear();
    }

    public <H extends PluginHook> Optional<H> findHook(Class<H> type) {
        PluginHook hook = hooks.get(type);
        if (hook == null || !hook.enabled()) {
            return Optional.empty();
        }
        return Optional.of(type.cast(hook));
    }

    public <H extends PluginHook> H requireHook(Class<H> type) {
        return findHook(type).orElseThrow(() -> new IllegalStateException("Hook not available: " + type.getSimpleName()));
    }
}
