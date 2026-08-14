package bm.b0b0b0.soulCrates.hook;

import org.bukkit.plugin.java.JavaPlugin;

public abstract class HookProvider<H extends PluginHook> {

    private final String pluginName;
    private final String description;
    private final String url;
    private final boolean silent;

    protected HookProvider(String pluginName, String description, String url, boolean silent) {
        this.pluginName = pluginName;
        this.description = description;
        this.url = url;
        this.silent = silent;
    }

    public String pluginName() {
        return pluginName;
    }

    public String description() {
        return description;
    }

    public String url() {
        return url;
    }

    public boolean silent() {
        return silent;
    }

    public boolean isAvailable(JavaPlugin plugin) {
        return plugin.getServer().getPluginManager().getPlugin(pluginName) != null;
    }

    public boolean isEnabled(JavaPlugin plugin) {
        return plugin.getServer().getPluginManager().isPluginEnabled(pluginName);
    }

    public abstract H createHook(JavaPlugin plugin) throws Exception;
}
